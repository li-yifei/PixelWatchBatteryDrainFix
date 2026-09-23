package io.github.li_yifei.pixelwatchbatterydrainfix;

import android.os.SystemClock;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * A per-association circuit breaker for the Pixel Watch Companion's CDM transport.
 *
 * <p>This module never changes an attestation response, key, certificate, or verification
 * result. It suppresses a new system-data transport request only after the companion app has
 * attempted to attach the same association repeatedly in a short window.</p>
 */
public final class WatchTransportCircuitBreaker extends XposedModule {
    private static final String TAG = "WatchCdmBreaker";
    private static final String COMPANION_PACKAGE =
            "com.google.android.apps.wear.companion";
    private static final String PERSISTENT_PROCESS =
            "com.google.android.apps.wear.companion:persistent";
    private static final String SERVICE_CLASS =
            "com.google.android.libraries.wear.companion.companiondevicemanager.service."
                    + "WearCompanionDeviceService";
    private static final String CDM_CLASS = "android.companion.CompanionDeviceManager";

    // ActionRequest.REQUEST_TRANSPORT and ActionRequest.OP_ACTIVATE on Android 17.
    private static final int REQUEST_TRANSPORT = 2;
    private static final int OP_ACTIVATE = 0;

    // Three attach attempts within 15 seconds opens a five-minute circuit breaker.
    private static final int MAX_ATTACHES_IN_WINDOW = 3;
    private static final long ATTACH_WINDOW_MS = 15_000L;
    private static final long COOLDOWN_MS = 5 * 60_000L;

    private final Map<Integer, CircuitState> states = new HashMap<>();
    private final Map<Integer, CircuitState> requestStates = new HashMap<>();
    private final java.util.Set<Integer> deactivated = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private boolean targetProcess;
    private boolean hooksInstalled;

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        targetProcess = PERSISTENT_PROCESS.equals(param.getProcessName());
        if (targetProcess) {
            log("loaded in " + param.getProcessName());
        }
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!targetProcess
                || hooksInstalled
                || !COMPANION_PACKAGE.equals(param.getPackageName())) {
            return;
        }

        hooksInstalled = true;
        try {
            int actionHooks = hookActionRequests(param.getClassLoader());
            int attachHooks = hookSystemDataAttach(param.getClassLoader());
            // Verified against Companion 5.0.0.958206140: this suspend function returns
            // Boolean and opens /cdm/channel/secure_transport for both request origins.
            Class<?> controller = Class.forName("wsa", false, param.getClassLoader());
            int entryHooks = 0;
            for (Method method : controller.getDeclaredMethods()) {
                if (method.getName().equals("e") && method.getParameterCount() == 3
                        && method.getParameterTypes()[0] == String.class
                        && method.getParameterTypes()[1].getName().equals("android.companion.AssociationInfo")
                        && method.getReturnType() == Object.class) {
                    hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                            .setId("watch-cdm-protective-entry")
                            .intercept(chain -> {
                                log("protective mode: declined CDM channel creation");
                                return Boolean.FALSE;
                            });
                    entryHooks++;
                }
            }
            log("protective channel entry hooks=" + entryHooks);
            log("installed " + actionHooks + " action hook(s) and " + attachHooks
                    + " attach hook(s)");
        } catch (Throwable error) {
            hooksInstalled = false;
            log("hook installation failed", error);
        }
    }

    /** Blocks further activation callbacks while a circuit is open. */
    private int hookActionRequests(ClassLoader classLoader) throws ClassNotFoundException {
        Class<?> serviceClass = Class.forName(SERVICE_CLASS, false, classLoader);
        int count = 0;
        for (Method method : serviceClass.getDeclaredMethods()) {
            if (!"onActionRequested".equals(method.getName())
                    || method.getParameterCount() != 2) {
                continue;
            }
            hook(method)
                    .setPriority(XposedInterface.PRIORITY_HIGHEST)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .setId("watch-cdm-action-request")
                    .intercept(this::interceptActionRequest);
            count++;
        }
        if (count == 0) {
            throw new NoSuchMethodError(SERVICE_CLASS + ".onActionRequested(AssociationInfo, ActionRequest)");
        }
        return count;
    }

    /** Records actual attachment attempts and opens the circuit on rapid repetition. */
    private int hookSystemDataAttach(ClassLoader classLoader) throws ClassNotFoundException {
        Class<?> managerClass = Class.forName(CDM_CLASS, false, classLoader);
        int count = 0;
        for (Method method : managerClass.getDeclaredMethods()) {
            if (!"attachSystemDataTransport".equals(method.getName())
                    || method.getParameterCount() < 1) {
                continue;
            }
            hook(method)
                    .setPriority(XposedInterface.PRIORITY_HIGHEST)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .setId("watch-cdm-attach-system-data-transport")
                    .intercept(this::interceptSystemDataAttach);
            count++;
        }
        if (count == 0) {
            throw new NoSuchMethodError(CDM_CLASS + ".attachSystemDataTransport");
        }
        return count;
    }

    private Object interceptActionRequest(XposedInterface.Chain chain) throws Throwable {
        if (chain.getArgs().size() != 2) {
            return chain.proceed();
        }
        Object request = chain.getArg(1);
        if (!Integer.valueOf(REQUEST_TRANSPORT).equals(invokeNoArg(request, "getAction"))) {
            return chain.proceed();
        }
        int associationId = associationId(chain.getArg(0));
        if (associationId < 0) {
            return chain.proceed();
        }
        int operation = (Integer) invokeNoArg(request, "getOperation");
        if (operation == 1) {
            deactivated.add(associationId);
            android.content.Context context = (android.content.Context) chain.getThisObject();
            Object manager = context.getSystemService(android.content.Context.COMPANION_DEVICE_SERVICE);
            manager.getClass().getMethod("detachSystemDataTransport", int.class)
                    .invoke(manager, associationId);
            ClassLoader loader = request.getClass().getClassLoader();
            Class<?> resultClass = Class.forName("android.companion.ActionResult", false, loader);
            Class<?> builderClass = Class.forName("android.companion.ActionResult$Builder", false, loader);
            int deactivated = resultClass.getField("RESULT_DEACTIVATED").getInt(null);
            Object builder = builderClass.getConstructor(int.class, int.class)
                    .newInstance(REQUEST_TRANSPORT, deactivated);
            Object result = builderClass.getMethod("build").invoke(builder);
            manager.getClass().getMethod("notifyActionResult", int.class, resultClass)
                    .invoke(manager, associationId, result);
            log("honored transport deactivation association=" + associationId);
            return null;
        }
        if (operation == OP_ACTIVATE) deactivated.remove(associationId);
        synchronized (requestStates) {
            CircuitState state = requestStates.computeIfAbsent(associationId, ignored -> new CircuitState());
            long timestamp = now();
            if (state.operation != operation) {
                state.operation = operation;
                state.attachTimes.clear();
                state.openUntilMs = 0;
            }
            if (timestamp < state.openUntilMs) return null;
            while (!state.attachTimes.isEmpty()
                    && timestamp - state.attachTimes.peekFirst() > ATTACH_WINDOW_MS) {
                state.attachTimes.removeFirst();
            }
            state.attachTimes.addLast(timestamp);
            if (state.attachTimes.size() >= MAX_ATTACHES_IN_WINDOW) {
                state.attachTimes.clear();
                state.openUntilMs = timestamp + COOLDOWN_MS;
                log("request storm suppressed association=" + associationId + " operation=" + operation);
                return null;
            }
        }
        if (!isTransportActivation(request) || !isCircuitOpen(associationId, now())) {
            return chain.proceed();
        }

        log("suppressed REQUEST_TRANSPORT activation for association=" + associationId
                + " while cooldown is active");
        // onActionRequested is void. Omitting proceed prevents a new Wearable ChannelClient stream
        // and a new CompanionDeviceManager.attachSystemDataTransport request.
        return null;
    }

    private Object interceptSystemDataAttach(XposedInterface.Chain chain) throws Throwable {
        if (chain.getArgs().isEmpty() || !(chain.getArg(0) instanceof Integer)) {
            return chain.proceed();
        }

        int associationId = (Integer) chain.getArg(0);
        if (deactivated.contains(associationId)) {
            for (Object arg : chain.getArgs()) {
                if (arg instanceof java.io.Closeable) {
                    try { ((java.io.Closeable) arg).close(); }
                    catch (java.io.IOException error) { log("closing deactivated stream", error); }
                }
            }
            log("closed late attach for deactivated association=" + associationId);
            return null;
        }
        if (!recordAttachAndShouldBlock(associationId, now())) {
            return chain.proceed();
        }

        log("opened circuit and suppressed attach for association=" + associationId);
        // attachSystemDataTransport is void. Omitting proceed prevents system_server from
        // constructing another SecureTransport for this association.
        return null;
    }

    private boolean isTransportActivation(Object request) {
        try {
            Object action = invokeNoArg(request, "getAction");
            Object operation = invokeNoArg(request, "getOperation");
            return action instanceof Integer
                    && operation instanceof Integer
                    && (Integer) action == REQUEST_TRANSPORT
                    && (Integer) operation == OP_ACTIVATE;
        } catch (Throwable error) {
            log("could not inspect ActionRequest", error);
            return false;
        }
    }

    private int associationId(Object association) {
        try {
            Object value = invokeNoArg(association, "getId");
            return value instanceof Integer ? (Integer) value : -1;
        } catch (Throwable error) {
            log("could not inspect AssociationInfo", error);
            return -1;
        }
    }

    private static Object invokeNoArg(Object receiver, String name) throws ReflectiveOperationException {
        return receiver.getClass().getMethod(name).invoke(receiver);
    }

    private static long now() {
        return SystemClock.elapsedRealtime();
    }

    private boolean recordAttachAndShouldBlock(int associationId, long timestamp) {
        synchronized (states) {
            CircuitState state = states.computeIfAbsent(associationId, ignored -> new CircuitState());
            if (timestamp < state.openUntilMs) {
                return true;
            }

            while (!state.attachTimes.isEmpty()
                    && timestamp - state.attachTimes.peekFirst() > ATTACH_WINDOW_MS) {
                state.attachTimes.removeFirst();
            }
            state.attachTimes.addLast(timestamp);

            if (state.attachTimes.size() < MAX_ATTACHES_IN_WINDOW) {
                return false;
            }

            state.attachTimes.clear();
            state.openUntilMs = timestamp + COOLDOWN_MS;
            return true;
        }
    }

    private boolean isCircuitOpen(int associationId, long timestamp) {
        synchronized (states) {
            CircuitState state = states.get(associationId);
            return state != null && timestamp < state.openUntilMs;
        }
    }

    private void log(String message) {
        Log.i(TAG, message);
        log(Log.INFO, TAG, message);
    }

    private void log(String message, Throwable error) {
        Log.e(TAG, message, error);
        log(Log.ERROR, TAG, message, error);
    }

    private static final class CircuitState {
        private int operation = -1;
        private final ArrayDeque<Long> attachTimes = new ArrayDeque<>();
        private long openUntilMs;
    }
}

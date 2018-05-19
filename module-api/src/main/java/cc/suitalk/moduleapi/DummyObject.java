package cc.suitalk.moduleapi;

import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by albieliang on 2018/5/9.
 */

public class DummyObject {

    private static final String TAG = "Api.DummyObject";

    public static boolean isDummy(Object o) {
        return o instanceof DummyMarker;
    }

    public static <T> T newInstance(@NonNull Class<T> interfaceClass) {
        if (!interfaceClass.isInterface()) {
            Log.e(TAG, String.format("the class(%s) is not a interface", interfaceClass));
            return null;
        }
        try {
            DummyInvocationHandler handler = new DummyInvocationHandler();
            Object instance = Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                    new Class<?>[]{interfaceClass, DummyMarker.class}, handler);
            Log.i(TAG, String.format("dummy instance(%s) for class(%s)", instance, interfaceClass));
            return (T) instance;
        } catch (Throwable e) {
            Log.e(TAG, android.util.Log.getStackTraceString(e));
        }
        return null;
    }

    private interface DummyMarker {
    }

    private static class DummyInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Class<?> returnType = method.getReturnType();
            if (returnType.equals(Integer.TYPE)) {
                return 0;
            }
            if (returnType.equals(Long.TYPE)) {
                return 0L;
            }
            if (returnType.equals(Double.TYPE)) {
                return 0.0;
            }
            if (returnType.equals(Float.TYPE)) {
                return 0.f;
            }
            if (returnType.equals(Short.TYPE)) {
                return 0;
            }
            if (returnType.equals(Byte.TYPE)) {
                return 0;
            }
            if (returnType.equals(Character.TYPE)) {
                return 0;
            }
            if (returnType.equals(Boolean.TYPE)) {
                return false;
            }
            return null;
        }
    }
}

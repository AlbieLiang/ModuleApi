package cc.suitalk.moduleapi;

import android.support.annotation.NonNull;
import android.util.Log;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Map;

import cc.suitalk.moduleapi.extension.Api;
import cc.suitalk.moduleapi.extension.annotation.DummyWhenNull;
import cc.suitalk.moduleapi.extension.annotation.InjectClass;

/**
 * Created by albieliang on 2018/2/6.
 */

public class ModuleApi {

    private static final String TAG = "Api.ModuleApi";

    private static final Map<Class<?>, Object> sMap = new HashMap<>();

    public static <T extends Api> T get(@NonNull Class<T> tClass) {
        Assert.assertNotNull(tClass);
        Object o = sMap.get(tClass);
        InjectClass injectClass = null;
        if (o == null && (injectClass = tClass.getAnnotation(InjectClass.class)) != null) {
            o = ReflectUtils.newInstance(injectClass.value());
            if (o != null) {
                set(tClass, o);
            }
        }
        if (o == null && tClass.getAnnotation(DummyWhenNull.class) != null) {
            return DummyObject.newInstance(tClass);
        }
        return (T) o;
    }

    public static <T extends Api> boolean set(@NonNull Class<T> tClass, @NonNull Object impl) {
        Assert.assertNotNull(tClass);
        Assert.assertNotNull(impl);
        if (!tClass.isAssignableFrom(impl.getClass())) {
            Log.i(TAG, String.format("the impl Object(%s) is not a instance of the class '%s'", impl, tClass));
            return false;
        }
        sMap.put(tClass, impl);
        return true;
    }

    public static <T extends Api> T remove(@NonNull Class<T> tClass) {
        Assert.assertNotNull(tClass);
        return (T) sMap.remove(tClass);
    }
}

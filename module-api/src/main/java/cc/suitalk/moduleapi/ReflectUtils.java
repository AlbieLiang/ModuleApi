package cc.suitalk.moduleapi;

import android.util.Log;

/**
 * Created by albieliang on 2016/3/28.
 */

class ReflectUtils {

    private static final String TAG = "Api.ReflectUtils";

    public static<T> T newInstance(String classStr) {
        if (classStr != null && classStr.length() > 0) {
            try {
                Class<?> c = Class.forName(classStr);
                return (T) c.newInstance();
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "reflect error : " + e.getMessage());
            } catch (IllegalAccessException e) {
                Log.e(TAG, "reflect error : " + e.getMessage());
            } catch (InstantiationException e) {
                Log.e(TAG, "reflect error : " + e.getMessage());
            }
        }
        return null;
    }
}

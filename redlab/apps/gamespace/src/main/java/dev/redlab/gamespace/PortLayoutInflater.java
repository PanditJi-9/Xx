package dev.redlab.gamespace;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Keeps the original layouts usable after the port repackaged the sources.
 *
 * <p>The original XML names its custom views as {@code cn.nubia.gamelauncher.*}, but the ported
 * sources for that package live under {@code dev.redlab.gamespace.*}. Editing the layouts would
 * break the "original assets, verbatim" rule and would be undone the next time the materials are
 * staged, so the redirect happens at inflation time instead: every tag in the original prefix is
 * looked up under the port prefix.
 *
 * <p>Only that one prefix is redirected. Other original packages ({@code cn.nubia.tgk},
 * {@code cn.nubia.gamecenter}, {@code com.zte}) keep their names in the ported sources, so their
 * tags resolve through the normal inflater.
 */
public final class PortLayoutInflater {

    private static final String ORIGINAL_PREFIX = "cn.nubia.gamelauncher.";
    private static final String PORT_PREFIX = "dev.redlab.gamespace.";

    private PortLayoutInflater() {
    }

    /** Installs the redirect on an activity's own inflater, before any layout is inflated. */
    public static void install(LayoutInflater inflater) {
        inflater.setFactory2(new LayoutInflater.Factory2() {
            @Override
            public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
                return create(name, context, attrs);
            }

            @Override
            public View onCreateView(String name, Context context, AttributeSet attrs) {
                return create(name, context, attrs);
            }
        });
    }

    /** Returns null for anything this factory does not own, so the default inflater handles it. */
    private static View create(String name, Context context, AttributeSet attrs) {
        if (!name.startsWith(ORIGINAL_PREFIX)) {
            return null;
        }
        String target = PORT_PREFIX + name.substring(ORIGINAL_PREFIX.length());
        try {
            Class<?> type = Class.forName(target, false, context.getClassLoader());
            if (!View.class.isAssignableFrom(type)) {
                return null;
            }
            return instantiate(type, context, attrs);
        } catch (ClassNotFoundException notPorted) {
            // Let the default inflater report the missing tag under its original name.
            return null;
        } catch (ReflectiveOperationException broken) {
            throw new IllegalStateException("Cannot create " + target + " for <" + name + ">", broken);
        }
    }

    private static View instantiate(Class<?> type, Context context, AttributeSet attrs)
            throws ReflectiveOperationException {
        try {
            Constructor<?> two = type.getConstructor(Context.class, AttributeSet.class);
            return (View) two.newInstance(context, attrs);
        } catch (NoSuchMethodException noTwo) {
            // Fall through to the other constructor shapes the platform accepts.
        }
        try {
            Constructor<?> three = type.getConstructor(Context.class, AttributeSet.class, int.class);
            return (View) three.newInstance(context, attrs, 0);
        } catch (NoSuchMethodException noThree) {
            Constructor<?> one = type.getConstructor(Context.class);
            return (View) one.newInstance(context);
        } catch (InvocationTargetException wrapped) {
            throw wrapped;
        }
    }
}

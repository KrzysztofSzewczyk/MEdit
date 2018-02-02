
package medit.BPP.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;

import medit.BPP.core.BPPValue;

public class BPPReflection {

	private static LinkedList<String> packages = new LinkedList<>();

	@SuppressWarnings("unchecked")
	public static boolean existsField(final Object classInstance, final String fieldName) {
		// ~ Public Field
		try {
			final Class clazz = classInstance instanceof Class ? (Class) classInstance : classInstance.getClass();
			clazz.getField(fieldName);

			return true;

		} catch (final SecurityException e) {

		} catch (final NoSuchFieldException e) {

		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public static boolean existsSubroutine(final Object classInstance, final String methodName,
			final BPPValue... args) {
		// ~ Public Subroutine
		try {
			final Class clazz = classInstance instanceof Class ? (Class) classInstance : classInstance.getClass();
			if (args != null) {
				final LinkedList<Class> classes = new LinkedList<>();
				for (final BPPValue value : args)
					classes.add(value.getType());
				clazz.getMethod(methodName, classes.toArray(new Class[] {}));
			} else
				clazz.getMethod(methodName, new Class[] {});

			return true;
		} catch (final NoSuchMethodException nsfe) {

		}

		return false;
	}

	public static String fullIdentifier(final String className) {
		for (final String pkg : BPPReflection.packages)
			try {

				Class.forName(pkg + "." + className);
				return pkg + "." + className;

			} catch (final ClassNotFoundException e) {
			}

		return null;
	}

	public static Object getFieldObject(final Object classInstance, final String fieldName) {
		// ~ Public Field
		try {
			final Field field = classInstance instanceof Class ? ((Class) classInstance).getField(fieldName)
					: classInstance.getClass().getField(fieldName);
			return field.get(classInstance);
		} catch (final IllegalAccessException iae) {

		} catch (final NoSuchFieldException nsfe) {

		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public static Object invokeStaticSubroutine(final Object classInstance, final String methodName,
			final BPPValue... args) {
		try {
			final Class clazz = classInstance instanceof Class ? (Class) classInstance : classInstance.getClass();
			if (args != null) {
				final LinkedList<Class> params = new LinkedList<>();
				for (final BPPValue arg : args)
					params.add(arg.getType());
				final Method method = clazz.getMethod(methodName, params.toArray(new Class[] {}));
				final LinkedList<Object> values = new LinkedList<>();
				for (final BPPValue arg : args)
					values.add(arg.getValue());
				return method.invoke(classInstance, values.toArray(new Object[] {}));
			} else {
				final Method method = clazz.getMethod(methodName, new Class[] {});
				return method.invoke(classInstance, new Object[] {});
			}

		} catch (final SecurityException se) {

		} catch (final NoSuchMethodException nsme) {

		} catch (final IllegalArgumentException iae) {

		} catch (final IllegalAccessException iae) {

		} catch (final InvocationTargetException ate) {

		}

		return null;
	}

	public static Class makeObject(final String className) {
		try {
			return Class.forName(className);
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void pushPackage(final String packageName) {
		BPPReflection.packages.add(packageName);
	}
}

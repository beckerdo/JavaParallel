package com.example;

import java.io.File;
import java.lang.Class;
import java.lang.reflect.*;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import org.junit.runners.ParentRunner;
import org.reflections.Reflections;

/**
 * A Java introspection tool that documents a number of classes.
 * <p>
 * 
 * @author <a href="mailto:dan@danbecker.info">Dan Becker </a>.
 */
@SuppressWarnings("rawtypes")
public final class DocumentClasses {
	/** Pool of sites */
	public static final List<String> CLASSNAMES = Arrays.asList(
			"java.lang.Runnable", "java.util.concurrent.Callable",
			"java.util.concurrent.Executor", "java.util.concurrent.Future",
			"java.util.concurrent.CompletionService",
			"java.util.concurrent.Executors",
			"java.util.concurrent.AbstractExecutorService");
	public static final Random random = new Random();

	/** Run this tool. */
	public static final void main(String... aArgs) {
		DocumentClasses doc = new DocumentClasses();
		try {
			for (String className : CLASSNAMES) {
				log("Class name=" + className);
				Class clazz = Class.forName(className);
				
				Method [] instanceMethods = getInstanceMethods( clazz );
				log("   Instance Methods:");
				for ( int i = 0; i < instanceMethods.length; i++ ) {
					Method method = instanceMethods[ i ];
				    log("   " + i + ". " + method.getName() );
				}

				Method [] supportedMethods = getSupportedMethods( clazz );
				log("   Supported Methods:");
				for ( int i = 0; i < supportedMethods.length; i++ ) {
					Method method = supportedMethods[ i ];
				    log("   " + i + ". " + method.getName() );
				}				

				Class [] supers = getSuperclasses( clazz );
				log("   Super classes:");
				for ( int i = 0; i < supers.length; i++ ) {
					Class superClass = supers[ i ];
				    log("   " + i + ". " + superClass.getName() );
				}

				Class [] interfaces = getInterfaces( clazz );
				log("   Interfaces:");
				for ( int i = 0; i < interfaces.length; i++ ) {
					Class myInterface = interfaces[ i ];
				    log("   " + i + ". " + myInterface.getName() );
				}

//				Reflections reflections = new Reflections("java.util.concurrent");
//				Set<Class<?>> classes = reflections.getSubTypesOf(clazz);
//				if (classes.size() > 0) {
//					log("   SubTypes:");
//					int i = 0;
//					for ( Class thisClass : classes ) {
//					    log("   " + i++ + ". " + thisClass.getName() );
//						
//					}
//				}
				log( "   Subclasses:");
				find( "java.util.concurrent", clazz );
				log( "   Class location:");
				which( clazz.getName() );			}
		} catch (Throwable ex) {
			log("Exception occured: " + ex);
			ex.printStackTrace();
		}
		log("Done.");
	}

	private static void log(Object aMsg) {
		System.out.println(String.valueOf(aMsg));
	}

	public static String getTypeName(Class cls) {
		if (!cls.isArray()) {
			return cls.getName();
		} else {
			return getTypeName(cls.getComponentType()) + "[]";
		}
	}

	/**
	 * Returns an array of the superclasses of cls.
	 * 
	 * @return java.lang.Class[]
	 * @param cls java.lang.Class
	 */
	public static Class[] getSuperclasses(Class cls) {
		int i = 0;
		for (Class x = cls.getSuperclass(); x != null; x = x.getSuperclass())
			i++;
		Class[] result = new Class[i];
		i = 0;
		for (Class x = cls.getSuperclass(); x != null; x = x.getSuperclass())
			result[i++] = x;
		return result;
	}

	/**
	 * Returns an array of the interfaces of cls.
	 * 
	 * @return java.lang.Class[]
	 * @param cls java.lang.Class
	 */
	public static Class[] getInterfaces(Class cls) {
		return cls.getInterfaces();
	}
	
	/**
	 * Returns an array of the instance variables of the the specified class.
	 * An instance variable is defined to be a non-static field that is declared
	 * by the class or inherited.
	 * 
	 * @return java.lang.Field[]
	 * @param cls java.lang.Class
	 */
	public static Field[] getInstanceVariables(Class cls) {
		List accum = new LinkedList();
		while (cls != null) {
			Field[] fields = cls.getDeclaredFields();
			for (int i = 0; i < fields.length; i++) {
				if (!Modifier.isStatic(fields[i].getModifiers())) {
					accum.add(fields[i]);
				}
			}
			cls = cls.getSuperclass();
		}
		Field[] retvalue = new Field[accum.size()];
		return (Field[]) accum.toArray(retvalue);
	}

	/**
	 * Returns an array of fields that are the declared instance variables of
	 * cls. An instance variable is a field that is not static.
	 * 
	 * @return java.lang.reflect.Field[]
	 * @param cls java.lang.Class
	 */
	public static Field[] getDeclaredIVs(Class cls) {
		Field[] fields = cls.getDeclaredFields();
		// Count the IVs
		int numberOfIVs = 0;
		for (int i = 0; i < fields.length; i++) {
			if (!Modifier.isStatic(fields[i].getModifiers()))
				numberOfIVs++;
		}
		Field[] declaredIVs = new Field[numberOfIVs];
		// Populate declaredIVs
		int j = 0;
		for (int i = 0; i < fields.length; i++) {
			if (!Modifier.isStatic(fields[i].getModifiers()))
				declaredIVs[j++] = fields[i];
		}
		return declaredIVs;
	}

	/**
	 * Return an array of the supported instance variables of this class. A
	 * supported instance variable is not static and is either declared or
	 * inherited from a superclass.
	 * 
	 * @return java.lang.reflect.Field[]
	 * @param cls
	 *            java.lang.Class
	 */
	public static Field[] getSupportedIVs(Class cls) {
		if (cls == null) {
			return new Field[0];
		} else {
			Field[] inheritedIVs = getSupportedIVs(cls.getSuperclass());
			Field[] declaredIVs = getDeclaredIVs(cls);
			Field[] supportedIVs = new Field[declaredIVs.length
					+ inheritedIVs.length];
			for (int i = 0; i < declaredIVs.length; i++) {
				supportedIVs[i] = declaredIVs[i];
			}
			for (int i = 0; i < inheritedIVs.length; i++) {
				supportedIVs[i + declaredIVs.length] = inheritedIVs[i];
			}
			return supportedIVs;
		}
	}

	/**
	 * Returns an array of the methods that are not static.
	 * 
	 * @return java.lang.reflect.Method[]
	 * @param cls
	 *            java.lang.Class
	 */
	public static Method[] getInstanceMethods(Class cls) {
		List instanceMethods = new ArrayList();
		for (Class c = cls; c != null; c = c.getSuperclass()) {
			Method[] methods = c.getDeclaredMethods();
			for (int i = 0; i < methods.length; i++)
				if (!Modifier.isStatic(methods[i].getModifiers()))
					instanceMethods.add(methods[i]);
		}
		Method[] ims = new Method[instanceMethods.size()];
		for (int j = 0; j < instanceMethods.size(); j++)
			ims[j] = (Method) instanceMethods.get(j);
		return ims;
	}

	/**
	 * Returns an array of methods to which instances of this class respond.
	 * 
	 * @return java.lang.reflect.Method[]
	 * @param cls
	 *            java.lang.Class
	 */
	public static Method[] getSupportedMethods(Class cls) {
		return getSupportedMethods(cls, null);
	}

	/**
	 * This method retrieves the modifiers of a Method without the unwanted
	 * modifiers specified in the second parameter. Because this method uses
	 * bitwise operations, multiple unwanted modifiers may be specified by
	 * bitwise or.
	 * 
	 * @return int
	 * @param m
	 *            java.lang.Method
	 * @param unwantedModifiers
	 *            int
	 */
	public static int getModifiersWithout(Method m, int unwantedModifiers) {
		int mods = m.getModifiers();
		return (mods ^ unwantedModifiers) & mods;
	}

	/**
	 * Returns a Method that has the signature specified by the calling
	 * parameters.
	 * 
	 * @return Method
	 * @param cls
	 *            java.lang.Class
	 * @param name
	 *            String
	 * @param paramTypes
	 *            java.lang.Class[]
	 */
	public static Method getSupportedMethod(Class cls, String name,
			Class[] paramTypes) throws NoSuchMethodException {
		if (cls == null) {
			throw new NoSuchMethodException();
		}
		try {
			return cls.getDeclaredMethod(name, paramTypes);
		} catch (NoSuchMethodException ex) {
			return getSupportedMethod(cls.getSuperclass(), name, paramTypes);
		}
	}

	/**
	 * Returns a Method array of the methods to which instances of the specified
	 * respond except for those methods defined in the class specifed by limit
	 * or any of its superclasses. Note that limit is usually used to eliminate
	 * them methods defined by java.lang.Object.
	 * 
	 * @return Method[]
	 * @param cls
	 *            java.lang.Class
	 * @param limit
	 *            java.lang.Class
	 */
	// start extract getSupportedMethods
	public static Method[] getSupportedMethods(Class cls, Class limit) {
		Vector supportedMethods = new Vector();
		for (Class c = cls; c != limit; c = c.getSuperclass()) {
			Method[] methods = c.getDeclaredMethods();
			for (int i = 0; i < methods.length; i++) {
				boolean found = false;
				for (int j = 0; j < supportedMethods.size(); j++)
					if (equalSignatures(methods[i],
							(Method) supportedMethods.elementAt(j))) {
						found = true;
						break;
					}
				if (!found)
					supportedMethods.add(methods[i]);
			}
		}
		Method[] mArray = new Method[supportedMethods.size()];
		for (int k = 0; k < mArray.length; k++)
			mArray[k] = (Method) supportedMethods.elementAt(k);
		return mArray;
	}

	/**
	 * This field is initialized with a method object for the equalSignatures
	 * method. This is an optimization in that selectMethods can use this field
	 * instead of calling getMethod each time it is called.
	 */
	static private Method equalSignaturesMethod;

	static {
		Class[] fpl = { Method.class, Method.class };
		try {
			equalSignaturesMethod = DocumentClasses.class.getMethod(
					"equalSignatures", fpl);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Determines if the signatures of two method objects are equal. In Java, a
	 * signature comprises the method name and the array of of formal parameter
	 * types. For two signatures to be equal, the method names must be the same
	 * and the formal parameters must be of the same type (in the same order).
	 * 
	 * @return boolean
	 * @param m1
	 *            java.lang.Method
	 * @param m2
	 *            java.lang.Method
	 */
	public static boolean equalSignatures(Method m1, Method m2) {
		if (!m1.getName().equals(m2.getName()))
			return false;
		if (!Arrays.equals(m1.getParameterTypes(), m2.getParameterTypes()))
			return false;
		return true;
	}

	/**
	 * Return a string that represents the signature of the specified method.
	 * 
	 * @return String
	 * @param m
	 *            java.lang.Method
	 */
	public static String signatureToString(Method m) {
		return m.getName() + "("
				+ formalParametersToString(m.getParameterTypes()) + ")";
	}

	/**
	 * Returns a string that can be used as a formal parameter list for a method
	 * that has the parameter types of the specified array.
	 * 
	 * @return String
	 * @param pts
	 *            java.lang.Class[]
	 */
	public static String formalParametersToString(Class[] pts) {
		String result = "";
		for (int i = 0; i < pts.length; i++) {
			result += getTypeName(pts[i]) + " p" + i;
			if (i < pts.length - 1)
				result += ",";
		}
		return result;
	}

	/**
	 * Returns a string that is an actual parameter list that matches the formal
	 * parameter list produced by formalParametersToString.
	 * 
	 * @return String
	 * @param pts
	 *            java.lang.Class[]
	 */
	public static String actualParametersToString(Class[] pts) {
		String result = "";
		for (int i = 0; i < pts.length; i++) {
			result += "p" + i;
			if (i < pts.length - 1)
				result += ",";
		}
		return result;
	}

	/**
	 * Returns a String that represents the header for a constructor.
	 * 
	 * @return String
	 * @param c
	 *            java.lang.Constructor
	 */
	public static String headerToString(Constructor c) {
		String mods = Modifier.toString(c.getModifiers());
		if (mods.length() == 0)
			return headerSuffixToString(c);
		else
			return mods + " " + headerSuffixToString(c);
	}

	/**
	 * Returns a String that represents the header suffix for a constructor. The
	 * term "header suffix" is not a standard Java term. We use it to mean the
	 * Java header without the modifiers.
	 * 
	 * @return String
	 * @param c
	 *            java.lang.Constructor
	 */
	public static String headerSuffixToString(Constructor c) {
		String header = signatureToString(c);
		Class[] eTypes = c.getExceptionTypes();
		if (eTypes.length != 0)
			header += " throws " + classArrayToString(eTypes);
		return header;
	}

	/**
	 * Returns a String that represents the signature for a constructor.
	 * 
	 * @return String
	 * @param c
	 *            java.lang.Constructor
	 */
	public static String signatureToString(Constructor c) {
		return c.getName() + "("
				+ formalParametersToString(c.getParameterTypes()) + ")";
	}

	/**
	 * Returns a String that represents the header of a method.
	 * 
	 * @return String
	 * @param m
	 *            java.lang.Method
	 */
	public static String headerToString(Method m) {
		String mods = Modifier.toString(m.getModifiers());
		if (mods.length() == 0)
			return headerSuffixToString(m);
		else
			return mods + " " + headerSuffixToString(m);
	}

	/**
	 * Returns a String that represents the suffix of the header of a method.
	 * The suffix of a header is not a standard Java term. We use the term to
	 * mean the Java header without the method modifiers.
	 * 
	 * @return String
	 * @param m
	 *            java.lang.Method
	 */
	public static String headerSuffixToString(Method m) {
		String header = getTypeName(m.getReturnType()) + " "
				+ signatureToString(m);
		Class[] eTypes = m.getExceptionTypes();
		if (eTypes.length != 0) {
			header += " throws " + classArrayToString(eTypes);
		}
		return header;
	}

	/**
	 * Returns a String that is a comma separated list of the typenames of the
	 * classes in the array pts.
	 * 
	 * @return String
	 * @param pts
	 *            java.lang.Class[]
	 */
	public static String classArrayToString(Class[] pts) {
		String result = "";
		for (int i = 0; i < pts.length; i++) {
			result += getTypeName(pts[i]);
			if (i < pts.length - 1)
				result += ",";
		}
		return result;
	}

	/**
	 * Turns true if and only if the header suffixes of the two specified
	 * methods are equal. The header suffix is defined to be the signature, the
	 * return type, and the exception types.
	 * 
	 * @return boolean
	 * @param m1
	 *            java.lang.Method
	 * @param m2
	 *            java.lang.Method
	 */
	public static boolean equalsHeaderSuffixes(Method m1, Method m2) {
		if (m1.getReturnType() != m2.getReturnType())
			return false;
		if (!Arrays.equals(m1.getExceptionTypes(), m2.getExceptionTypes()))
			return false;
		return equalSignatures(m1, m2);
	}

	/**
	 * Creates constructor with the signature of c and a new name. It adds some
	 * code after generating a super statement to call c. This method is used
	 * when generating a subclass of class that declared c.
	 * 
	 * @return String
	 * @param c
	 *            java.lang.Constructor
	 * @param name
	 *            String
	 * @param code
	 *            String
	 */
	public static String createRenamedConstructor(Constructor c, String name,
			String code) {
		Class[] pta = c.getParameterTypes();
		String fpl = formalParametersToString(pta);
		String apl = actualParametersToString(pta);
		Class[] eTypes = c.getExceptionTypes();
		String result = name + "(" + fpl + ")\n";
		if (eTypes.length != 0)
			result += "    throws " + classArrayToString(eTypes) + "\n";
		result += "{\n    super(" + apl + ");\n" + code + "}\n";
		return result;
	}

	/**
	 * Returns a String that is formatted as a Java method declaration having
	 * the same header as the specified method but with the code parameter
	 * substituted for the method body.
	 * 
	 * @return String
	 * @param m
	 *            java.lang.Method
	 * @param code
	 *            String
	 */
	public static String createReplacementMethod(Method m, String code) {
		Class[] pta = m.getParameterTypes();
		String fpl = formalParametersToString(pta);
		Class[] eTypes = m.getExceptionTypes();
		String result = m.getName() + "(" + fpl + ")\n";
		if (eTypes.length != 0)
			result += "    throws " + classArrayToString(eTypes) + "\n";
		result += "{\n" + code + "}\n";
		return result;
	}

	/**
	 * Returns a string for a cooperative override of the method m. That is, The
	 * string has the same return type and signature as m but the body has a
	 * super call that is sandwiched between the strings code1 and code2.
	 * 
	 * @return String
	 * @param m
	 *            java.lang.Method
	 * @param code1
	 *            String
	 * @param code2
	 *            String
	 */
	public static String createCooperativeWrapper(Method m, String code1,
			String code2) {
		Class[] pta = m.getParameterTypes();
		Class retType = m.getReturnType();
		String fpl = formalParametersToString(pta);
		String apl = actualParametersToString(pta);
		Class[] eTypes = m.getExceptionTypes();
		String result = retType.getName() + " " + m.getName() + "(" + fpl
				+ ")\n";
		if (eTypes.length != 0)
			result += "    throws " + classArrayToString(eTypes) + "\n";
		result += "{\n" + code1 + "    ";
		if (retType != void.class)
			result += retType.getName() + " cooperativeReturnValue = ";
		result += "super." + m.getName() + "(" + apl + ");\n";
		result += code2;
		if (retType != void.class)
			result += "    return cooperativeReturnValue;\n";
		result += "}\n";
		return result;
	}

	/**
	 * Returns all of the interfaces that cls inherits. There are no duplicates.
	 * The order of the returned array is that of a breadth-first search.
	 * 
	 * @return Class[]
	 * @param cls
	 *            java.lang.Class
	 * @param limit
	 *            java.lang.Class
	 */
	public static Class[] getAllInterfaces(Class cls, Class limit) {
		assert (limit == null || (!limit.isInterface() && !limit.isPrimitive()));
		List<Class> cq = new LinkedList<Class>();
		if (cls.isInterface())
			cq.add(cls);
		for (Class x = cls; x != null && x != limit; x = x.getSuperclass())
			getInterfaceSubtree(x, cq);
		return (Class[]) cq.toArray();
	}

	// stop extract getAllInterfaces
	public static Class[] getAllInterfaces(Class cls) {
		return getAllInterfaces(cls, null);
	}

	/**
	 * Adds to cq all the interfaces in the subtree above cls. Because cq is a
	 * UQueueOfClass, duplicates are eliminated by the add operations.
	 * 
	 * @param cls
	 *            java.lang.Class
	 * @param cq
	 *            UQueue
	 */
	private static void getInterfaceSubtree(Class cls, List<Class> cq) {
		Class[] iArray = cls.getInterfaces();
		for (int j = 0; j < iArray.length; j++) {
			cq.add(iArray[j]);
			getInterfaceSubtree(iArray[j], cq);
		}
	}

	/**
	 * Returns the method object for the unique method named mName. If no such
	 * method exists, a null is returned. If there is more than one such method,
	 * a runtime exception is thrown.
	 * 
	 * @return Method
	 * @param cls
	 *            java.lang.Class
	 * @param mName
	 *            String
	 */
	public static Method getUniquelyNamedMethod(Class cls, String mName) {
		Method result = null;
		Method[] mArray = cls.getDeclaredMethods();
		for (int i = 0; i < mArray.length; i++)
			if (mName.equals(mArray[i].getName())) {
				if (result == null)
					result = mArray[i];
				else
					throw new RuntimeException("name is not unique");
			}
		return result;
	}

	/**
	 * Finds the first (from the bottom of the inheritance hierarchy) field with
	 * the specified name. Note that Class.getField returns only public fields.
	 * 
	 * @return Field
	 * @param cls
	 *            java.lang.Class
	 * @param name
	 *            String
	 */
	public static Field findField(Class cls, String name)
			throws NoSuchFieldException {
		if (cls != null) {
			try {
				return cls.getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				return findField(cls.getSuperclass(), name);
			}
		} else {
			throw new NoSuchFieldException();
		}
	}

	/**
	 * Produces an array of all the fields in a class, each of which has all of
	 * the modifiers indicated by the int parameter mustHave and none of the
	 * modifiers indicated by the int parameter mustNotHave. Note that
	 * "all the fields in a class" means the class and all of its superclasses.
	 * For example, selectFields(x,0,Modifier.FINAL) returns all the fields that
	 * are not final.
	 * 
	 * @return Field[]
	 * @param cls
	 *            java.lang.Class
	 * @param mustHave
	 *            int
	 * @param mustNotHave
	 *            int
	 */
	public static Field[] selectFields(Class cls, int mustHave, int mustNotHave) {
		List<Field> fq = new LinkedList<Field>();
		Class[] ca = selectAncestors(cls, 0, 0);
		for (int j = 0; j < ca.length; j++) {
			Field[] fa = ca[j].getDeclaredFields();
			for (int i = 0; i < fa.length; i++) {
				int mods = fa[i].getModifiers();
				if (((mods & mustHave) == mustHave)
						&& ((mods & mustNotHave) == 0))
					fq.add(fa[i]);
			}
		}
		return (Field[]) fq.toArray();
	}

	/**
	 * Produces an array of all the methods in a class, each of which has all of
	 * the modifiers indicated by the int parameter mustHave and none of the
	 * modifiers indicated by the int parameter mustNotHave. Note that
	 * "all the methods in a class" means the class and all of its superclasses.
	 * In addition, selectMethod returns only one entry for any specific
	 * signature.
	 * 
	 * For example, selectMethods(x,Modifier.PUBLIC,0) should return the same
	 * result as x.getMethods() (is this true? selectMethods eliminates
	 * duplicates).
	 * 
	 * IMPORTANT NOTE: selectMethods is not sensitive to semantic issues. For
	 * example, in selectMethod( cls, Modifier.ABSTRACT, 0 ) if a method is
	 * declared abstract by a superclass of cls and subsequently defined by cls,
	 * that method appears in the returned array.
	 * 
	 * @see getMethodsLackingImplementation.
	 * 
	 * @return Method[]
	 * @param cls
	 *            java.lang.Class
	 * @param mustHave
	 *            int
	 * @param mustNotHave
	 *            int
	 */
	public static Method[] selectMethods(Class cls, int mustHave,
			int mustNotHave) {
		return (Method[]) selectMethods0(cls, mustHave, mustNotHave, null)
				.toArray();
	}

	public static Method[] selectMethods(Class cls, int mustHave,
			int mustNotHave, Class limit) {
		return (Method[]) selectMethods0(cls, mustHave, mustNotHave, limit)
				.toArray();
	}

	private static UQueue selectMethods0(Class cls, int mustHave,
			int mustNotHave, Class limit) {
		UQueue mq = new UQueue(Method.class, equalSignaturesMethod);
		Class[] ca = selectAncestors(cls, 0, 0, limit);
		for (int j = 0; j < ca.length; j++) {
			Method[] ma = ca[j].getDeclaredMethods();
			for (int i = 0; i < ma.length; i++) {
				int mods = ma[i].getModifiers();
				if (((mods & mustHave) == mustHave)
						&& ((mods & mustNotHave) == 0))
					mq.add(ma[i]);
			}
		}
		return mq;
	}

	/**
	 * Returns an array of all of the abstract methods of class cls. A method
	 * that is declared abstract in a superclass or superinterface of cls is not
	 * abstract if an implementation has been provided for the method.
	 * 
	 * Note that the implementation uses UQueueOfMethod, which is generated by
	 * the C2C framework. The UQueueOfMethod is constructed with a Method object
	 * for the equality test used by the queue.
	 * 
	 * @return Method[]
	 * @param cls
	 *            java.lang.Class
	 */
	public static Method[] getMethodsLackingImplementation(Class cls) {
		UQueue imq = selectMethods0(cls, 0, Modifier.ABSTRACT, null);
		UQueue amq = selectMethods0(cls, Modifier.ABSTRACT, 0, null);
		UQueue rmq = new UQueue(Method.class, equalSignaturesMethod);
		for (int i = 0; i < amq.size(); i++) {
			Method rm = (Method) amq.elementAt(i);
			if (!imq.contains(rm))
				rmq.add(rm);
		}
		return (Method[]) rmq.toArray();
	}

	/**
	 * Returns an array of all the ancestor classes and interfaces of cls. The
	 * class cls is first. The array is a topological sort.
	 * 
	 * @return Class[]
	 * @param cls
	 *            java.lang.Class
	 * @param mustHave
	 *            int
	 * @param mustNotHave
	 *            int
	 */
	// start extract selectAncestors

	public static Class[] selectAncestors(Class cls, int mustHave,
			int mustNotHave) {
		return selectAncestors(cls, mustHave, mustNotHave, null);
	}

	/**
	 * Returns an array of all the ancestor classes and interfaces of cls. The
	 * class cls is first. The array is a topological sort. The search of
	 * superclasses stop at the limit. If the limit is not a superclass of cls,
	 * this method fails with a NullPointerException.
	 * 
	 * @return Class[]
	 * @param cls
	 *            java.lang.Class
	 * @param mustHave
	 *            int
	 * @param mustNotHave
	 *            int
	 * @param limit
	 *            java.lang.Class
	 */
	public static Class[] selectAncestors(Class cls, int mustHave,
			int mustNotHave, Class limit) {
		List<Class> cq = new LinkedList<Class>();
		if (!cls.isInterface()) {
			for (Class x = cls; x != limit; x = x.getSuperclass()) {
				int mods = x.getModifiers();
				if (((mods & mustHave) == mustHave)
						&& ((mods & mustNotHave) == 0))
					cq.add(x);
			}
		}
		Class[] ca = getAllInterfaces(cls, limit);
		for (int i = 0; i < ca.length; i++) {
			int mods = ca[i].getModifiers();
			if (((mods & mustHave) == mustHave) && ((mods & mustNotHave) == 0))
				cq.add(ca[i]);
		}
		return (Class[]) cq.toArray();
	}

	/**
	 * Returns the Method object for the first declaration of the method the
	 * signature matching the specified name and fpl. The search order has
	 * classes preceed interfaces. This means that if the method object returned
	 * is abstract, no implementation is defined for the method. If no match is
	 * found, null is returned.
	 * 
	 * @return Method
	 * @param cls
	 *            java.lang.Class
	 * @param name
	 *            String
	 * @param fpl
	 *            java.lang.Class[]
	 */
	public static Method getMethod(Class cls, String name, Class[] fpl) {
		for (Class c = cls; c != null; c = c.getSuperclass()) {
			try {
				return c.getDeclaredMethod(name, fpl);
			} catch (NoSuchMethodException e) {
			}
		}
		Class[] ca = getAllInterfaces(cls);
		for (int i = 0; i < ca.length; i++) {
			try {
				return ca[i].getDeclaredMethod(name, fpl);
			} catch (NoSuchMethodException e) {
			}
		}
		return null;
	}

    public static void find(String pckgname, Class parent) {
        // Translate the package name into an absolute path
        String name = new String(pckgname);
        if (!name.startsWith("/")) {
            name = "/" + name;
        }        
        name = name.replace('.','/');
        
        // Get a File object for the package
        URL url = DocumentClasses.class.getResource(name);
        if ( null != url ) {
        File directory = new File(url.getFile());
        // New code
        // ======
        if (directory.exists()) {
            // Get the list of the files contained in the package
            String [] files = directory.list();
            for (int i=0;i<files.length;i++) {
                 
                // we are only interested in .class files
                if (files[i].endsWith(".class")) {
                    // removes the .class extension
                    String classname = files[i].substring(0,files[i].length()-6);
                    try {
                        // Try to create an instance of the object
                        Object o = Class.forName(pckgname+"."+classname).newInstance();
                        // if (o instanceof parent) {
                        if (o.getClass().isAssignableFrom(parent)) {
                        // if ( parent.isAssignableFrom(o.getClass())) {
                            System.out.println(classname);
                        }
                    } catch (ClassNotFoundException cnfex) {
                        System.err.println(cnfex);
                    } catch (InstantiationException iex) {
                        // We try to instantiate an interface
                        // or an object that does not have a 
                        // default constructor
                    } catch (IllegalAccessException iaex) {
                        // The class is not public
                    }
                }
            }
        }
        }
    }

    public static void which(String className) {   	
    	      if (!className.startsWith("/")) {
    	        className = "/" + className;
    	      }
    	      className = className.replace('.', '/');
    	      className = className + ".class";
    	
    	      java.net.URL classUrl =
    	        DocumentClasses.class.getResource(className);
    	
    	      if (classUrl != null) {
    	        System.out.println("Class '" + className +
    	          "' found in '" + classUrl.getFile() + "'");
    	      } else {
            System.out.println("\nClass '" + className +
    	          "' not found in \n'" +
    	          System.getProperty("java.class.path") + "'");
    	      }
        }
    
}
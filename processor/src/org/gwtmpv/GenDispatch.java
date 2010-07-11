package org.gwtmpv;

/** Marks a class as a specification for {@code XxxAction} and {@code XxxResult} classes. */
public @interface GenDispatch {
	String baseAction() default "";
	String baseResult() default "";

	/**
	 * True if the generated classes should implement GWT's {@code IsSerializable}.
	 * 
	 * <p>Set to <tt>false by default</tt></p>
	 * 
	 * @return true if the generated classes should implement GWT's {@code IsSerializable}
	 */
	boolean implementsIsSerializable() default false;
}

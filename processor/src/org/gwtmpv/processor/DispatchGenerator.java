package org.gwtmpv.processor;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import org.exigencecorp.aptutil.GenericSuffix;
import org.exigencecorp.aptutil.Prop;
import org.exigencecorp.aptutil.PropUtil;
import org.exigencecorp.aptutil.Util;
import org.gwtmpv.GenDispatch;

import joist.sourcegen.GClass;
import joist.sourcegen.GMethod;

public class DispatchGenerator {
	
	private static final String GWT_IS_SERIALIZABLE_CLASS_NAME = "com.google.gwt.user.client.rpc.IsSerializable";

	private final ProcessingEnvironment env;
	private final TypeElement element;
	private final GClass actionClass;
	private final GClass resultClass;
	private final GenericSuffix generics;

	public DispatchGenerator(ProcessingEnvironment env, TypeElement element) throws InvalidTypeElementException {
		if (!element.toString().endsWith("Spec")) {
			env.getMessager().printMessage(Kind.ERROR, "GenDispatch targets must end with a Spec suffix", element);
			throw new InvalidTypeElementException();
		}

		String dispatchBasePackage = env.getOptions().get("dispatchBasePackage");
		if (dispatchBasePackage == null) {
			// Auto-detect gwt-dispatch
			TypeElement gwtDispatchAction = env.getElementUtils().getTypeElement("net.customware.gwt.dispatch.shared.Action");
			TypeElement gwtpAction = env.getElementUtils().getTypeElement("com.philbeaudoin.gwtp.dispatch.shared.Action");
			if (gwtDispatchAction != null) {
				dispatchBasePackage = "net.customware.gwt.dispatch.shared";
			} else if (gwtpAction != null) {
				dispatchBasePackage = "com.philbeaudoin.gwtp.dispatch.shared";
			} else {
				dispatchBasePackage = "org.gwtmpv.dispatch.shared";
			}
		}

		this.env = env;
		this.generics = new GenericSuffix(element);
		String base = element.toString().replaceAll("Spec$", "");

		this.actionClass = new GClass(base + "Action" + generics.varsWithBounds);
		this.actionClass.getField("serialVersionUID").type("long").setStatic().setFinal().initialValue("1L");

		this.resultClass = new GClass(base + "Result" + generics.varsWithBounds);
		this.resultClass.getField("serialVersionUID").type("long").setStatic().setFinal().initialValue("1L");

		GenDispatch genDispatch = element.getAnnotation(GenDispatch.class);
		if (genDispatch.baseAction() != null && genDispatch.baseAction().length() > 0) {
			this.actionClass.baseClassName("{}<{}>", genDispatch.baseAction(), base + "Result" + generics.vars);
		} else {
			this.actionClass.implementsInterface("{}.Action<{}>", dispatchBasePackage, base + "Result" + generics.vars);
		}
		if (genDispatch.baseResult() != null && genDispatch.baseResult().length() > 0) {
			this.resultClass.baseClassName(genDispatch.baseResult());
		} else {
			this.resultClass.implementsInterface("{}.Result", dispatchBasePackage);
		}
		
		if ( genDispatch.implementsIsSerializable()) {
			this.actionClass.implementsInterface(GWT_IS_SERIALIZABLE_CLASS_NAME);
			this.resultClass.implementsInterface(GWT_IS_SERIALIZABLE_CLASS_NAME);
		}

		PropUtil.addGenerated(this.actionClass, DispatchGenerator.class);
		PropUtil.addGenerated(this.resultClass, DispatchGenerator.class);

		this.element = element;
	}

	public void generate() {
		generateDto(actionClass, PropUtil.getProperties(element, "in"));
		generateDto(resultClass, PropUtil.getProperties(element, "out"));
	}

	private void generateDto(GClass gclass, List<Prop> properties) {
		// move to GClass as a utility method
		GMethod cstr = gclass.getConstructor();
		for (Prop p : properties) {
			addFieldAndGetterAndConstructorArg(gclass, cstr, p.name, p.type);
		}
		if (properties.size() > 0) {
			// re-add the default constructor for serialization
			gclass.getConstructor().setProtected();
		}
		PropUtil.addHashCode(gclass, properties);
		PropUtil.addEquals(gclass, generics, properties);
		PropUtil.addToString(gclass, properties);
		Util.saveCode(env, gclass);
	}

	private void addFieldAndGetterAndConstructorArg(GClass gclass, GMethod cstr, String name, String type) {
		gclass.getField(name).type(type);
		gclass.getMethod("get" + Util.upper(name)).returnType(type).body.append("return this.{};", name);
		cstr.argument(type, name);
		cstr.body.line("this.{} = {};", name, name);
	}

}

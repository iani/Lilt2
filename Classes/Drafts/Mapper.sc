/* IZ Aug 7, 2013 (1:08 PM)

Encapsulate the spec, unmapped value and mapped value so that they are stored together, and so that mapping or unmapping only happens once, at value update time.

(
m = Mapper(\freq);
w = Window();
w.view.layout = m.numSlider;
w.front;
)

TODO: Consider: Adapter should use Notification with \value (!) as message. Reason is that we may want to change other aspects of a view, via the Mapper. For example: change clipHi and clipLo in response to change of spec. Enable or disable view.

*/


Mapper {
	var <name;
	var <>spec;
	var <value;
	var <unmappedValue;

	*new { | name, spec, value |
		^this.newCopyArgs(name, spec, value).init;
	}

	init {
		spec ?? { spec = name.asSpec ?? { ControlSpec(0, 1, \lin) } };
		value ?? { value = spec.default; };
		unmappedValue = spec.unmap(value);
	}

	value_ { | argValue = 0 |
		value = argValue;
		unmappedValue = spec.unmap(value);
		this.changed;
	}

	unmappedValue_ { | argUnmappedValue = 0 |
		unmappedValue = argUnmappedValue;
		value = spec.map(unmappedValue);
		this.changed;
	}

	numberBox { | width = 50 |
		^NumberBox().addModel(this).fixedWidth_(width);
	}

	slider { | orientation = \horizontal |
		^Slider().addModel(this).orientation_(orientation);
	}

	knob {
		^Knob().addModel(this);
	}

	numSlider { | labelWidth = 100 |
		^HLayout(
			StaticText().string_(name).fixedWidth_(labelWidth),
			this.numberBox,
			this.slider
		)
	}

	staticText {
		thisMethod.notYetImplemented;
	}

	updatingStaticText {
		thisMethod.notYetImplemented;
	}

}

Adapter {
	var <model, <view;

	*new { | model, view |
		^this.newCopyArgs(model, view).init;
	}

	init {
		model.addDependant(this);
		view.action = { this.sendValueToModel };
		view.onClose = {
			model.removeDependant(this);
			view.objectClosed;
		};
		this.update;
	}

	sendValueToModel { model.value = view.value }

	update { view.value = model.value }

	model_ { | argModel |
		model !? { model.removeDependant(this); };
		model = argModel;
		this.init;
	}

}

MappingAdapter : Adapter {

	sendValueToModel { model.unmappedValue = view.value }

	update { view.value = model.unmappedValue }

}


+ QSlider {
	addModel { | model |
		MappingAdapter(model, this);
	}
}

+ QKnob {
	addModel { | model |
		MappingAdapter(model, this);
	}
}

+ QNumberBox {
	addModel { | model |
		Adapter(model, this);
	}
}

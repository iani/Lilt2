/* IZ Aug 7, 2013 (1:08 PM)

Encapsulate the spec, unmapped value and mapped value so that they are stored together, and so that mapping or unmapping only happens once, at value update time.

(
w = Window();
w.view.layout = VLayout(*([\freq, \amp, \pan, \rq, \level] collect: { | n | Mapper(n).numSlider }));
w.front;
)

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
		this.broadcastValue;
	}

	broadcastValue {
		// subclasses add more to this
		this.changed(\value);
	}
	unmappedValue_ { | argUnmappedValue = 0 |
		unmappedValue = argUnmappedValue;
		value = spec.map(unmappedValue);
		this.broadcastValue;
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

	numSlider {
		^HLayout(
			this.staticText,
			this.numberBox,
			this.slider
		)
	}

	staticText { | labelWidth = 100 |
		^StaticText().string_(name).fixedWidth_(labelWidth);
	}

	updatingStaticText {
		thisMethod.notYetImplemented;
	}

}


SynthParameter : Mapper {
	var <synthModel;
	var <mapBusConnector;  // BusConnector: A bus to which this control may be mapped

	broadcastValue {
		synthModel.set(name, value);
		super.broadcastValue;
	}

}


Adapter {
	var <model, <view;

	*new { | model, view |
		^this.newCopyArgs(model, view).init;
	}

	init {
		this.addNotifier(model, \value, { this.update });
		view.action = { this.sendValueToModel };
		view.onClose = { view.objectClosed; };
		// Works even if onClose is overwritten by addNotifier:
		view.onObjectClosed({ this.objectClosed; });
		this.update;
	}

	sendValueToModel { model.value = view.value }

	update { view.value = model.value }

	model_ { | argModel |
		model !? { this.removeNotifier(this, \value); };
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

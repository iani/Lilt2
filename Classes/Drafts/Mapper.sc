/* IZ Aug 7, 2013 (1:08 PM)

Encapsulate the spec, unmapped value and mapped value so that they are stored together, and so that mapping or unmapping only happens once, at value update time.

*/


Mapper {
	var <value = 0;
	var <unmappedValue = 0;
	var <>spec;

	// To be considered:
//	var <model;
//	var <name;


	value_ { | argValue = 0 |
		value = argValue;
		unmappedValue = spec.unmap(value);
		// model.changed(name, this);
	}

	unmappedValue_ { | argUnmappedValue = 0 |
		unmappedValue = argUnmappedValue;
		value = spec.map(unmappedValue);
		// model.changed(name, this);
	}
}


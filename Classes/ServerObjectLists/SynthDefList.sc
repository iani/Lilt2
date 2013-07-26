/* IZ Jul 26, 2013 (11:41 PM)

Display a list of useful global SynthDefs.

Offer buttons / key commands for:

- Starting a SynthModel from a SynthDef and adding it to SynthList.default
- Just adding a SynthModel from a SynthDef to SynthList.default
- Opening a gui from a SynthModel from a SynthDef

*/

SynthDefList {
/*
SynthDefList.gui;
*/
	*initClass {
			StartUp add: {
			(PathName(PathName(PathName(this.filenameSymbol.asString).parentPath).parentPath
			).parentPath +/+ "SynthDefs/*.scd").pathMatch do: _.load;
			(Platform.userAppSupportDir +/+ "SynthDefs/*.scd").pathMatch do: _.load;
		}
	}

	*gui {
		var window, list;
		window = Window("SynthDefs", Rect(0, 300, 200, 450)).front;
		window.view.layout = VLayout(
			list = ListView().font_(Font.default.size_(10))
			.items_(
				SynthDescLib.global.synthDescs.keys.asArray.collect(_.asString).reject({ | n |
					"freqScope*".matchRegexp(n) or: {
						"system_*".matchRegexp(n)
					}
				}).sort
			);
		)
	}

}
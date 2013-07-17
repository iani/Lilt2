/* IZ Jul 3, 2013 (2:46 PM)

=== Notes for porting the synth-linking procedure mechanism from Lilt1 to Lilt2 ===

The algorithms for cycle-checking and signal-diverting bus creation of Lilt1 as well as the class organization are good solutions, so they will be adopted here. Those parts shich relate to the gui will be removed.

=== Principles ===

The output of a Synth (w) is linked to an input of another synth (r) via a Bus (l).
Synth w _writes_ its output to the bus (hence it is called the "writer").
Synth r _reads_ an input from the bus (hence it is called the "reader").

Many synths can write to the same bus, and many synths can read from the same bus.
However, when many readers nr want to share a single bus with many writers nw, care must be taken so that the bus only contains the mixed outputs from exactly those writers that are required by all readers nr. This is explained in more detail in the document Connecting Scripts (Implementation Notes).pdf.

Tbe handling of the synth processes (w and r) is implemented through classes ControlPatch (for control busses) and AudioPatch (for audio busses).

The handling of the bus and its connections to synth processes is handled by class IOBus. A IOBus holds a single bus, one or more writers (in dictionary "inputs"), and one or more readers (in dictionary "outputs"). Thus, theoretically the following configurations are possible:

1. one-to-one:

writer1 -> IOBus -> reader1

2. one-to-many:

                      / reader1
                     /
writer1 -> IOBus -- reader 2
                     \
                      \ reader 3

3. many-to-one

writer1 \
         \
writer2 -- IOBus -> reader1
         /
writer3 /

4. many-to-many

writer1 \             / reader1
         \           /
writer2 -- IOBus -- reader2
         /           \
writer3 /             \ reader3


However, as indicated above, in the case of many-to-many, to ensure that all readers only receive the outputs of the writers that they specify, the following rule is applied:

A. If an input bus A of a reader r has more than one readers, then to add a writer w to r one must:
1. Create a new bus B, that will connect the output of w to the input of r
2. Remove r from the outputs of A
3. Copy the contents of A to B

B Conversely, if an output bus A of a writer w has more than one writers, then to add a reader r to w one must:

1. Create a new bus B that will connect the output of w to the input of r.
2. Remove w from the inputs of A.
3. Copy the contents of B to A.

The copying of contents (signal) from one bus to another is managed by class BusBranch. A BusBranch creates a copy of the signal from one bus on a new bus which is the new "branch" bus.

    Bus 1
     |
     |\
     | \
     |  \ Bus 2 ("branch")
     |   |
     |   |

A direct consequence of the above rules is that whatever the number and complexity of interconnections added, the resulting graph will always have busses interconnected in one of the following two ways:

(a) The bus has one writer only and one or more readers
 or:
(b) The bus has one or more writers and one reader only.

Thus, the class components involved in the general scheme of many-to-many links between synths are:

1. SynthPatch: handle the processes of "writer" and "reader" synths, and provide variables holding inputs (Input instances), outputs (Output instance), and ControlMappers for mapping Controls to Buses.

2. Output: links the output of a synth to a IOBus instance.

3. IOBus: links one or more Output or BusBranch instances to one or more Input or BusBranch instances

4. Input: links one IOBus instance to an input of a synth.

5. BusBranch: is inserted between Output and IOBus or between IOBus and Input to copy bus contents as required by the rule explained above. (Implementation note: Can be implemented as subclass of AudioPatch and ControlPatch?


=== Classes ===

* SynthPatch

Holds the SynthModel which produces the output, plus inputs, outputs and controls for linking it to other patches.

Inputs are implemented as an Array of Input instances.

Outputs are implemented as an Array of Output instances.

* ControlMapper (previously Parameter class)

Superclass of Input, Output.
Its subclasses implement the linking of an input or output to other outputs or inputs via busses.

var <patch;	// the Audio/Control/SynthPatch to which this parameter belongs


Note: IODesc also holds variables defining the rate, the numberOfChannels and the UGen as well as the name of the Control which sets its value. The name of the control is stored in startingChannel.

var <bus;		// a IOBus with links to all my writers
var <controlName; // An instance of ControlName, from the synthDesc of the model.

* Input

var <descriptor; // An instance of IODesc, from the synthDesc of the model.

* Output

// var <name;		// name of parameter controlled. Is set by name_
var <output; 	// a IOBus with links to all my readers


* IOBus : Bus
var <writers; // array of parameters of scripts writing to this bus
var <readers; // array of parameters of scripts reading from this bus

=== Bus instance creation and connections to inputs and outputs ===


*/


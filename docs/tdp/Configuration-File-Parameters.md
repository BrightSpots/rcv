# RCTab v1.3.2 Section 24 - Tabulator Command Line Instructions v.1.1.2

> **RCTab v1.3.2 Section 24 - Tabulator Command Line Instructions
> v.1.1.2** document is solely for use in the State of California. This
> document can be expanded or updated as is necessary or required. Where
> relevant, this document refers to specific sections and requirements
> of the California Voting System Standards. Any recommendations listed
> in this document should not supersede user jurisdiction procedures or
> other controlling governance entities.

To run the tabulation for a config file via the command line, users must
open a terminal, navigate to the root directory of the unzipped software
build, and use a command similar to the one that launches the GUI,
except with different arguments. The final argument is the name of the
config file to be tabulated. All tabulation will be done in the terminal
window. RCTab’s user interface will not appear. If command line launch
is successful, RCTab will tabulate a contest according to the
requirements of the configuration file in the command line argument.
Following are detailed steps for how to launch the RCTab in the command
line.

On Windows:

1.  Open Start Menu

2.  Type Command Prompt. Hit enter.

3.  Command Prompt will launch. The screenshot below displays an example
    of Command Prompt.

<img src="../media/image37.png"
style="width:6.59722in;height:3.48611in" />

1.  The user needs to direct Command Prompt to the folder where RCTab is
    installed. Type cd. Insert the folder name: locate the folder rcv
    under the folder rctab\_v1.3.1\_windows in File Explorer.

2.  Click on the rcv folder and while continuing to hold the button
    down, drag the file to the Command Prompt window and place after cd.

    1.  Example: cd C:\RCTab\rctab\_v1.3.1\_windows\rcv

3.  Hit enter. The screenshot displays the result of a successful
    execution of previous steps.

<img src="../media/image29.png"
style="width:6.59722in;height:3.47222in" />

1.  Type in bin\java -m
    network.brightspots.rcv/network.brightspots.rcv.Main -cli
    \[filename\]

    1.  Filename should be the name of the configuration file for the
        RCTab to use to process the contest. Example:
        "C:\RCTab\rctab\_v1.3.1\_windows\rcv\sample\_input\sample\_interactive\_tiebreak"

    2.  Screenshot shows an example of correct command prompt entry for
        this example.

<img src="../media/image23.png"
style="width:6.59722in;height:3.5in" />

1.  Filename can be copied over into the Command Prompt by finding the
    configuration file in File Explorer, clicking on the file, and
    dragging the file over to the Command Prompt window.

2.  Press enter.

3.  Tabulation will run. Messages explaining the process of tabulation
    will appear in the Command Prompt window. Screenshot shows an
    example of messages sent during tabulation.

<img src="../media/image22.png"
style="width:6.25174in;height:3.51278in" />

1.  When tabulation is complete, Command Prompt will display the
    message: “INFO: Tabulation Session Completed.” Followed by a line
    stating, “Results written to: \[filepath\]”. The file path will be
    based on the information included in the Output Directory setting in
    the configuration file. See **<span class="mark">RCTab v1.3.2
    Section 08 - System Operations Procedures v.1.2.2</span>** and
    **<span class="mark">RCTab v1.3.2 Section 25 - Configuration File
    Parameters v.1.1.1</span>** for more information. See below for an
    example of messages sent at the end of successful tabulation.

<img src="../media/image31.png"
style="width:6.59722in;height:3.90278in" />

1.  If any of the above commands do not work, double check that you have
    copied them all over correctly, that you are pointing the command
    prompt to the correct directory, and that all filenames are
    correctly entered.

<table>
<colgroup>
<col style="width: 16%" />
<col style="width: 11%" />
<col style="width: 48%" />
<col style="width: 23%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4"><strong>Document Revision History</strong></th>
</tr>
<tr class="odd">
<th><strong>Date</strong></th>
<th><strong>Version</strong></th>
<th><strong>Description</strong></th>
<th><strong>Author</strong></th>
</tr>
<tr class="header">
<th>9/26/2023</th>
<th>1.1.2</th>
<th>Use different example path outside of user paths</th>
<th>Mathew Ruberg</th>
</tr>
<tr class="odd">
<th>04/28/2023</th>
<th>1.1.1</th>
<th>Updated to reflect v. 1.3.1</th>
<th>Kelly Sechrist</th>
</tr>
<tr class="header">
<th>04/04/2023</th>
<th>1.1.0</th>
<th>Updated to Reflect RCTab v.1.3.0</th>
<th>Rene Rojas</th>
</tr>
<tr class="odd">
<th>01/20/2022</th>
<th>1.1.0</th>
<th>Updated URCVT to RCTab and removed NY from document.</th>
<th>Rene Rojas</th>
</tr>
<tr class="header">
<th>04/24/2021</th>
<th>1.0.0</th>
<th>Command Line Instructions</th>
<th>Louis Eisenberg</th>
</tr>
</thead>
<tbody>
</tbody>
</table>


# RCTab v1.3.2 Section 28 - Post-Election Audit & Clearing RCTab from System v.1.1.1

> **RCTab v1.3.2 Section 28 - Post-Election Audit & Clearing RCTab from
> System v.1.1.1** document is solely for use in the State of
> California. This document can be expanded or updated as is necessary
> or required. Where relevant, this document refers to specific sections
> and requirements of the California Voting System Standards. Any
> recommendations listed in this document should not supersede user
> jurisdiction procedures or other controlling governance entities.

## Post-Election Audit Preparation for RCTab

<span class="mark">In order to run a post-election audit of RCTab, users
will need to gather up all relevant log files and understand the
information included in those logs. This guide will describe where to
find the relevant log files and how to read those log files in any audit
procedure.</span>

### Retrieving the Operator log(s)

<span class="mark">The operator log is a .log file that includes all
information from the black log box at the bottom of the user interface,
as shown at the bottom of the screenshot here:</span>

<img src="../media/image45.png"
style="width:5.69792in;height:1.45741in" />

<span class="mark">The operator log is updated any time RCTab generates
any message to send through that log box. Note that messages are
generated internally by the software, then saved to the operator log
file and shown to the user in the log box.</span>

<span class="mark">On Windows, this log file is saved to the folder
where RCTab is launched from - in practice, that means the bin folder
within RCTab’s install location (the bin folder is where the user
launches RCTab using the rcv.bat launcher file). Operator logs are saved
in the format rcv\_\*.log: rcv\_0.log, rcv\_1.log, and so on. Each log
has a maximum size of 50MB, and once a log reaches 50MB, a new
rcv\_\*.log file is created. This log file is continuously updated as a
user interacts with RCTab. Users should retrieve all such logs when
conducting a post-election audit.</span>

### Reading the operator log

<span class="mark">The operator log includes any error messages sent to
the user in operation, as well as summary information about each
tabulation run through RCTab (how many CVRs have been read in, how many
votes each candidate got in each round, winners of each contest). Below
is a screenshot of a rcv\_0.log. Each line starts with a timestamp of
when the message was generated. There are a variety of messages in this
screenshot: informational messages letting the user know they've saved a
configuration file, information about a tabulated contest (how many
votes each candidate received in a round, the candidate names RCTab is
looking for in the CVR), and error messages about information RCTab
needs in order to run a ranked choice voting election (at the top of the
screen). Similar information will be repeated in the operator log file
every time a user runs a tabulation through RCTab.</span>

<span class="mark">This log can be used to determine how a user has
interacted with RCTab: any error messages they've received, summary
information of any time they've run a tabulation on the given
installation of RCTab, and other information about user interaction with
the software itself.</span>

<img src="../media/image52.png"
style="width:5.81166in;height:5.10938in" />

<span class="mark">All messages RCTab may send to a user are included in
**RCTab v1.3.2 Section 29 - RCTab Operator Log Messages v.1.2.2**. This
document categorizes messages by the labels given to them in RCTab’s
operation: INFO, WARNING, and SEVERE. Severe errors cause RCTab’s
tabulation to fail, and so suggested resolution steps for any severe
errors are provided in the document. Info and Warning messages convey
information about contest configuration files, contest tabulation
progress, CVR files, and other relevant information about tabulation. No
Info or Warning messages cause tabulation to fail, but Info and Warning
messages may be sent along with Severe messages to provide users with
potential resolution steps for any Severe errors.</span>

### Retrieving contest audit log(s)

<span class="mark">The audit log is a record of everything RCTab did to
process a given ranked choice voting contest. A new audit log is
produced each time the user runs a tabulation using the "Tabulate"
option in RCTab. Each audit log is saved to the output folder the user
selects in the Output Directory setting on the Output tab in the RCTab
user interface. The manufacturer suggests users save all files for a
contest to a folder named after that contest (for example, County
Commission November 2020, City Council District 5 April 2018). This will
make retrieving any audit logs straightforward.</span>

<span class="mark">Audit logs, like operator logs, have a maximum size
of 50MB. Once an audit log reaches 50MB, RCTab creates a new audit log
for the tabulation. Audit log files grow quickly because they include a
large amount of information on how every piece of data in a CVR file is
handled, so users should confirm that they have retrieved every audit
.log file from the relevant contest(s).</span>

<span class="mark">Audit logs are named according to the rule:</span>

<span class="mark">\[time\_stamp\]\_audit\_0.log where the timestamp is
created when the tabulation is triggered and used on all audit files for
a given tabulation.</span>

<span class="mark">For example, a tabulation that begins at 10:49:49 pm
on April 24, 2021, will produce an audit log named:
2021-04-24\_22-49-49\_audit\_0.log</span>

<span class="mark">When an audit log reaches 50MB size, it will be
renamed along with any other preceding log files. For example:</span>

<span class="mark">2021-04-24\_22-49-49\_audit\_0.log -&gt;
2021-04-24\_22-49-49\_audit\_1.log</span>

<span class="mark">2021-04-24\_22-49-49\_audit\_1.log -&gt;
2021-04-24\_22-49-49\_audit\_2.log</span>

<span class="mark">Then a new 2021-04-24\_22-49-49\_audit\_0.log file
will be created, and logging will continue.</span>

<span class="mark">Each time an audit log file is written to disk a
corresponding .hash file is also written. This hash file contains a
cryptographic hash that can be used to verify the contents on the
corresponding audit log file. See **RCTab v.1.3.1 Section 23 - Trusted
Build & Output Hash Verification - Windows OS v.1.2.2** for instructions
on how to use the hash file to verify audit logs.</span>

### Reading the audit log

<span class="mark">Audit logs include the configuration file settings
used in the tabulation, how every individual ballot/CVR record in the
given CVR file(s) for the contest got counted in each round, which
candidate got eliminated in each round of counting, and other details of
how the contest was counted. These logs also include all messages sent
in the log box on the Tabulator UI during contest tabulation. Below is a
screenshot example of information included in an audit log:</span>

<img src="../media/image53.png"
style="width:6.5in;height:5.84722in" />

<span class="mark">Each line starts with a timestamp of when the message
was generated.</span>

<span class="mark">At the top of the screenshot is the end of the
configuration file settings for this contest. In the middle, RCTab is
reading in all the CVR files for the contest and a list of all the
candidate names listed in the configuration file that is also found in
the CVR files. At the bottom is the start of ballot-by-ballot counting.
In the first round of counting, every individual ballot in the CVR
file(s) in a contest will be listed out here. This information displays
how RCTab read in each ballot and which candidate it counted the ballot
for in the first round. In all subsequent rounds, the audit log displays
information for each transferred ballot - which candidate the ballot
counted for in the previous round and which candidate the ballot counts
for in that new round (or the exhaust condition of the ballot - if an
overvote is reached or if a ballot runs out of rankings, for
example).</span>

<span class="mark">Audit logs close by noting where summary results
files were saved to in the case of a successful tabulation. In the case
of a tabulation that cannot be completed (because candidate names are
missing, or required rules are missing, or other issues), audit logs
close by noting the errors sent to a user. This same information will be
included in the operator log.</span>

<span class="mark">Audit log files can be used to check:</span>

-   <span class="mark">What counting rules were used in a tabulation
    using RCTab;</span>

-   <span class="mark">The list of candidates used in a tabulation using
    RCTab;</span>

-   <span class="mark">The total number of individual cast vote records
    included in all CVR export files used in a contest
    tabulation;</span>

-   <span class="mark">How every ballot in each CVR file was counted in
    each round of counting;</span>

-   <span class="mark">Summary information for votes each candidate
    received in each round of the contest;</span>

-   <span class="mark">How RCTab read every single cell of each CVR
    file;</span>

-   <span class="mark">Where all contest files were saved;</span>

-   <span class="mark">And errors leading to a failed tabulation.</span>

<span class="mark">This is how every line in a CVR file is first
displayed in RCTab:</span>

<span class="mark">2020-12-02 17:53:54 EST FINE: \[Round\] 1 \[CVR\]
4582 \[counted for\] Candidate 9 \[Raw Data\] \[4582, Precinct 5, Ballot
Style 5, Candidate 9, Candidate 4, undervote, undervote, Candidate 10,
undervote, undervote, overvote, undervote, Candidate 2\]</span>

<span class="mark">This can be broken down into three parts: the time
stamp, how the vote was counted in this round, and all the data RCTab
read for this individual cast vote record.</span>

<span class="mark">Time Stamp:</span>

<span class="mark">2020-12-02 17:53:54 EST FINE: This message was sent
at 5:53 pm on December 2, 2020.</span>

<span class="mark">Vote counted in this round:</span>

<span class="mark">\[Round\] 1 \[CVR\] 4582 \[counted for\] Candidate 9:
In round 1, CVR ID 4582 was counted for Candidate 9.</span>

<span class="mark">All CVR data read for this individual CVR:</span>

<span class="mark">\[Raw Data\] \[4582, Precinct 5, Ballot Style 5,
Candidate 9, Candidate 4, undervote, undervote, Candidate 10, undervote,
undervote, overvote, undervote, Candidate 2\]: This ballot was labeled
CVR \# 4582 in the CVR file for this contest. It was cast in precinct 5.
The ballot style was ballot style 5. The ballot used their rankings in
this order: Candidate 9, Candidate 4, undervote, undervote, Candidate
10, undervote, undervote, overvote, undervote, Candidate 2.</span>

<span class="mark">(Note: undervote means a ranking was left
blank).</span>

<span class="mark">Later in this same contest, after candidate 9 is
eliminated, the audit log displays this information:</span>

<span class="mark">2020-12-02 17:53:56 EST FINE: \[Round\] 4 \[CVR\]
4582 \[transferred to\] Candidate 4</span>

<span class="mark">This tells the user that the CVR listed above,
\#4582, transferred in Round 4 to Candidate 4.</span>

<span class="mark">Users can check multiple different factors using the
audit log and operator log to review the performance of RCTab
software.</span>

1.  <span class="mark">Using the audit .log, check that the total CVRs
    read into RCTab is equal to the total ballots cast according to the
    EMS used to export CVR files.</span>

2.  <span class="mark">Check the rcv\_0.log and any audit .logs for
    SEVERE errors and to see how users resolved those errors.</span>

3.  <span class="mark">Compare summary result information in the audit
    file(s) and the summary results files for a contest run through
    RCTab.</span>

4.  <span class="mark">Review the election data form used for a contest
    (see Section 11 - L&A Testing pages 7-9). Compare the information in
    this form to the configuration file used in a contest as reflected
    in the audit .log for that contest to confirm that the proper
    counting rules were used for the contest.</span>

5.  <span class="mark">Using audit .log files, compare how data in the
    CVR files was read into RCTab with the information included in CVR
    files exported from the relevant election management software. This
    data can also be cross-referenced with paper ballot data from
    precincts, using precinct information in the audit .log files and in
    CVR files from the election management software.</span>

6.  <span class="mark">If using the Tabulate by Precinct feature of
    RCTab, users can compare a hand count of ballots from a precinct to
    RCTab’s tabulation of those same ballots. The Tabulate by Precinct
    feature produces round-by-round results at the precinct level. These
    results display how ballots at the precinct level transferred in the
    contest as a whole, not a simulated round-by-round count in the
    precinct. A hand count could be conducted at the precinct level,
    following the elimination order in RCTab results, to check that
    RCTab counted each ballot properly in a given precinct.</span>

    1.  <span class="mark">Note that the Tabulate by Precinct feature
        identifies a winner. That identified winner is the winner of the
        contest overall, not necessarily the person who received the
        most votes in that specific precinct. This will be updated in a
        future release to identify the winner of the precinct.</span>

7.  <span class="mark">When summary files are written to disk the audit
    log also contains the text of the hash of those files. Confirming
    that the summary file hashes in the audit log matches the .hash
    files written to disk is another layer of security to prevent
    malicious editing of summary files.</span>

## Clearing RCTab from a System

<span class="mark">Note: Before following this procedure, ensure that
any materials that must be archived according to the jurisdiction’s
archiving requirements have been archived.</span>

<span class="mark">There are three types of files needed to remove RCTab
from your system: RCTab installation folders, any files you created when
using RCTab (configuration files, results files, audit files), and any
CVR files you save to the RCTab workstation.</span>

-   <span class="mark">Delete the RCTab installation folder (generically
    called rctab\_1.3.0\_windows after you unzip it) - this deletes
    RCTab itself. Delete any .zip file of RCTab as well.</span>

-   <span class="mark">Delete any folders where you have RCTab files
    (meaning configuration files, results files, and audit files) or CVR
    files saved. The simplest way to keep track of this is to set up an
    RCTab Files folder on the computer with RCTab, with subfolders for
    each contest/L&A process run through RCTab. The user could then save
    all relevant files for each contest (configuration file, results
    files, audit files, and CVR files) to those folders.</span>

<table>
<colgroup>
<col style="width: 15%" />
<col style="width: 14%" />
<col style="width: 51%" />
<col style="width: 18%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4"><strong>Document Revision History</strong></th>
</tr>
<tr class="odd">
<th>Date</th>
<th>Version</th>
<th>Description</th>
<th>Author</th>
</tr>
<tr class="header">
<th>09/26/2023</th>
<th>1.1.2</th>
<th>Include comparing the text of output file .hash files to the hash
text in the audit log as another auditable check</th>
<th>Mathew Ruberg</th>
</tr>
<tr class="odd">
<th>04/28/2023</th>
<th>1.1.1</th>
<th>Updated for RCTab v1.3.1</th>
<th>Melissa Hall</th>
</tr>
<tr class="header">
<th>04/06/2023</th>
<th>1.1.0</th>
<th>Updated to Reflect RCTab v.1.3.0</th>
<th>Rene Rojas</th>
</tr>
<tr class="odd">
<th>09/09/2021</th>
<th>1.0.1</th>
<th>Updated for general audience.</th>
<th>Melissa Hall</th>
</tr>
<tr class="header">
<th>05/14/2021</th>
<th>1.0.0</th>
<th>Post-Election Audit and Clearing URCVT from System</th>
<th>RCVRC</th>
</tr>
</thead>
<tbody>
</tbody>
</table>


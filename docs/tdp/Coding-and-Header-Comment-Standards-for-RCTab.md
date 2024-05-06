# RCTab v1.3.2 Section 30 - RCTab System Tab Hints v.1.1.1

> **RCTab v1.3.2 Section 30 - RCTab System Tab Hints v.1.1.1** document
> is solely for use in the State of California. This document can be
> expanded or updated as is necessary or required. Where relevant, this
> document refers to specific sections and requirements of the
> California Voting System Standards. Any recommendations listed in this
> document should not supersede user jurisdiction procedures or other
> controlling governance entities.

These hints are displayed in the user interface of RCTab to help users
navigate through each option available in RCTab.

## Hints for Contest Info Tab

<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th><p>The tabulator calculates results for one contest at a time,
rather than the results of several contests for an election all at once.
The information on this tab is for the particular contest you will be
tabulating.</p>
<p>These fields do not influence the computations. They are shown in the
final output file(s) to help connect the data (results) with the contest
the results belong to.</p>
<p>* Think long term, e.g. from the perspective of looking at the
results files 6 months after the election and wanting to be clear what
contest the results belong to.</p>
<p>* You may find it helpful to revisit this tab once you have done a
few test runs and see what the output looks like.</p>
<p>Contest Name (required): Enter a name to identify it.</p>
<p>Examples: City Council 2018, Board of Election Ward 13 2017, Mayor,
Referendum 289b</p>
<p>Contest Date (optional): The date on which the election for this
contest was run.</p>
<p>Contest Jurisdiction (optional): E.g.: Minneapolis, Eastpointe</p>
<p>Whether this is helpful may depend on what you put into the Contest
Name field</p>
<p>Contest Office (optional): E.g.: Mayor, County Clerk</p>
<p>Whether this is helpful may depend on what you put into the Contest
Name field</p>
<p>Rules Description (optional): What short description of this
configuration would help you remember in, say, six months what election
this specific rule configuration is for?</p></th>
</tr>
</thead>
<tbody>
</tbody>
</table>

## Hints for Candidates Tab 

<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th><p>Fill in the fields and click the Add button:</p>
<p>Name (required): E.g.: Dave Harris.</p>
<p>Code (optional): Some CVR files use codes in lieu of full candidate
name. E.g.: "JCD" or "14".</p>
<p>Excluded (optional): When checked, the candidate will be ignored
during tabulation. An example of when this might be used: a candidate
dropped out after the ballots were printed.</p></th>
</tr>
</thead>
<tbody>
</tbody>
</table>

## Hints for CVR Files Tab

<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th><p>The tabulator needs to know where each of your CVR files is and
how to interpret each of them. As you add files, it will build up a list
of files to use when it tabulates the results of this contest.</p>
<p>For each of your CVR files, provide the necessary information and
then use the Add button to add it to the list.</p>
<p>Provider (required): The vendor/machine that generated (produced) the
file. After you select the field, the tabulator will fill in as many of
the other fields as it can based on what it knows about that provider.
You can adjust those values as necessary.</p>
<p>Path (required): Location of the CVR file.</p>
<p>* Example: /Users/test data/2015-portland-mayor-cvr.xlsx</p>
<p>Contest ID (required for non-ES&amp;S): Some CVRs assign an ID label
to each contest in the CVR. The tabulator needs to know which contest is
being tabulated when multiple contests are included in one CVR. Enter
the ID of the contest being tabulated in this field.</p>
<p>First Vote Column (required for ES&amp;S): the column where the first
vote record is.</p>
<p>First Vote Row (required for ES&amp;S): the row where the first vote
record is.</p>
<p>ID Column (optional): The column the IDs are in. Not all CVR files
contain an ID column.</p>
<p>Precinct Column (required for ES&amp;S if you want to tabulate by
precinct): The column that contains the precinct.</p>
<p>Overvote Delimiter (optional, but must be blank if "Overvote Label"
is provided): If using a CVR in ES&amp;S style, overvotes can be
reflected in a CVR by displaying all candidates marked at a ranking.
Those candidate names will be differentiated from each other by a
delimiter, something like a vertical bar | or a slash /. If your
overvotes are delimited like this, enter the delimiter used in this
field. Note that ES&amp;S files may include only the label "overvote"
and no additional information, in which case the "Overvote Label" field
should be used instead.</p>
<p>Overvote Label (optional): Some CDF and ES&amp;S CVRs use a
particular word/phrase to indicate an overvote.</p>
<p>Undervote Label (optional): Some ES&amp;S CVRs use a particular
word/phrase to indicate an undervote.</p>
<p>Undeclared Write-in Label (optional): Some CVRs use a particular
word/phrase to indicate a write-in.</p>
<p>Treat Blank as Undeclared Write-in (optional): When checked, the
tabulator will interpret blank cells in this ES&amp;S CVR as votes for
undeclared write-ins.</p></th>
</tr>
</thead>
<tbody>
</tbody>
</table>

## Hints for Winning Rules Tab

<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th><p>Winner Election Mode (required): What process to use for
selecting winner(s) for this contest.</p>
<p>* Single-winner majority determines winner: Elects one winner.
Eliminate candidates one-by-one or using batch elimination until a
candidate emerges with a majority. Candidate with the most votes at the
end wins.</p>
<p>* Multi-winner allows only one winner per round: Elects multiple
winners. Elect and transfer the surplus vote of only the candidate with
the most votes if multiple candidates exceed the winning threshold in a
round of counting.</p>
<p>* Multi-winner allows multiple winners per round: Elects multiple
winners. Elect and transfer the surplus vote of all candidates crossing
the winning threshold if multiple candidates exceed the winning
threshold in a round of counting.</p>
<p>* Bottoms-up: Eliminate candidates until the desired number of
winners is reached, then stop. Bottoms up does not transfer surplus
votes.</p>
<p>* Bottoms-up using percentage threshold: Elects multiple winners.
Eliminate candidates until the remaining candidates have a vote share
equal to or greater than a specified percentage of the vote.</p>
<p>* Multi-pass IRV: Elects multiple winners. Eliminate candidates
one-by-one or using batch elimination until only two candidates remain.
Candidate with the most votes at the end wins. Run a new set of rounds
with any winning candidates ignored.</p>
<p>Maximum Number of Candidates That Can Be Ranked: How many rankings
each voter has in this contest.</p>
<p>Minimum Vote Threshold: The number of first-choice votes a candidate
must receive in order to remain in the race. Most jurisdictions do not
set a minimum vote threshold.</p>
<p>Use Batch Elimination: Batch elimination, or simultaneous elimination
of all candidates for whom it is mathematically impossible to be
elected, eliminates all candidates who cannot receive enough votes to
surpass the candidate with the next highest number of votes. Example: in
a six candidate contest with 200 votes, Candidate A has 80 votes,
Candidate B has 70, and the other four combined have 50. Because those
four candidates can never combine their votes to surpass Candidate B,
they can be batch eliminated. Available only when Winner Election Mode
is "Single-winner majority determines winner".</p>
<p>Continue until Two Candidates Remain: Single-winner ranked-choice
voting elections can stop as soon as a candidate receives a majority of
votes, even though 3 or more candidates may still be in the race.
Selecting this option will run the round-by-round count until only two
candidates remain, regardless of when a candidate wins a majority of
votes.</p>
<p>Tiebreak Mode (required): Ties in ranked-choice voting contests can
occur when eliminating candidates or when electing candidates.
Multi-winner contests can have ties between candidates who have both
crossed the threshold of election; in that case ties are broken to
determine whose surplus vote value transfers first. Tiebreak procedures
are set in law, either in the ranked-choice voting law used in your
jurisdiction or in the elections code more generally. Select the option
from this list that complies with law and procedure in your
jurisdiction.</p>
<p>* Random: Randomly select a tied candidate to eliminate or, in
multi-winner contests only, elect. Requires a random seed.</p>
<p>* Stop counting and ask: Pause count when a tie is reached. User is
prompted to select any tied candidate to eliminate or, in multi-winner
contests only, elect.</p>
<p>* Previous round counts (then random): The tied candidate with the
least votes in the previous round loses the tie. If there is a tie in
the previous round, the tie is broken randomly. Requires a random
seed.</p>
<p>* Previous round counts (then stop counting and ask): The tied
candidate with the least votes in the previous round loses the tie. If
there is a tie in the previous round, user is prompted to select any
tied candidate to eliminate or, in multi-winner contests only,
elect.</p>
<p>* Use candidate order in the config file: Use the order of candidates
in the config file to determine tiebreak results. Candidates lower in
the list lose the tiebreaker.</p>
<p>* Generate permutation: Generate a randomly ordered list of
candidates in the contest. Candidates lower in the permutation lose the
tiebreaker. Requires a random seed.</p>
<p>Random Seed (required if Tiebreak Mode is "Random", "Previous round
counts (then random)", or "Generate permutation"): Enter a positive or
negative integer to generate random orders.</p>
<p>Number of Winners: The number of seats to be filled in the
contest.</p>
<p>Percentage Threshold: The share of votes a candidate must have in
order to win. Candidates falling below this threshold are eliminated
one-by-one beginning with the candidate with the fewest votes. Available
only when Winner Election Mode is "Bottoms-up using percentage
threshold".</p>
<p>Threshold Calculation Method: The threshold of election is the number
of votes a candidate must receive in order to win election. There are
three primary ways to calculate the threshold of election in
multi-winner RCV contests. This will be set in law (either by statute or
regulation) in your jurisdiction. Available only when Winner Election
Mode is "Multi-winner allow only one winner per round" or "Multi-winner
allow multiple winners per round".</p>
<p>* Compute using most common threshold formula: The most common
threshold formula is calculated by dividing the number of votes by the
number of seats plus one, then adding one to that number. Fractions are
disregarded. This is also known as the Droop quota. Candidates must
receive this number of votes (or more) to win.</p>
<p>* Compute using HB Quota: The HB, or Hagenbach-Bischoff, Quota
divides the number of votes by the number of seats plus one, leaving
fractions. Candidates must receive more than this number of votes to
win.</p>
<p>* Compute using Hare Quota: The Hare quota divides the number of
votes by the number of seats. It requires candidates to receive that
number of votes (or more) to win.</p>
<p>Decimal Places for Vote Arithmetic (Multi-Winner Only): Sets how many
decimal places after the decimal point are used in surplus transfers and
in calculating the threshold.</p></th>
</tr>
</thead>
<tbody>
</tbody>
</table>

## Hints for Voter Error Rules Tab

<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th><p>The tabulator needs to know how to handle voter errors in your
jurisdiction. These requirements are typically included in statute or
regulation.</p>
<p>Overvote Rule (required): How to handle a ballot where a voter has
marked multiple candidates at the same ranking when that ballot is
encountered in the round-by-round count.</p>
<p>* Always skip to next rank: Skips over an overvote and goes to the
next validly-marked ranking on a ballot.</p>
<p>* Exhaust immediately: A ballot with an overvote exhausts when that
overvote is encountered in the rounds of counting.</p>
<p>* Exhaust if multiple continuing: If a voter has an overvote but only
one candidate at that overvote is still in the race when that overvote
is encountered, the ballot counts for that candidate. If multiple
candidates at the overvote are still in the race, the ballot
exhausts.</p>
<p>How Many Consecutive Skipped Ranks Are Allowed (required): How many
rankings in a row can a voter skip and still have later rankings count?
0 allows no skipped rankings. 1 allows voters to skip rankings one at a
time, but not more than 1 in a row, and so on.</p>
<p>Example: A voter could rank in 1, 3, 5 and not exhaust under this
rule, for example.</p>
<p>Exhaust on Multiple Ranks for the Same Candidate: When checked, the
tabulator will exhaust a ballot that includes multiple rankings for the
same candidate when that repeat ranking is reached.</p>
<p>Example: A voter ranks the same candidate 1st and 3rd, a different
candidate 2nd, and another candidate 4th. If their original first choice
and their second choice are eliminated, the ballot exhausts when it
reaches the repeat ranking in rank 3. The ranking in the 4th rank does
not count.</p></th>
</tr>
</thead>
<tbody>
</tbody>
</table>

## Hints for Output Tab

<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th><p>Tell the tabulator where results files go and what additional
results files you want.</p>
<p>Output directory: The location where results files will go. If no
value (or a relative path, like "output") is supplied, the location
where the config file is saved will be used as the base directory.
Absolute paths, like "C:\output" work too.</p>
<p>Tabulate by Precinct: Produce round-by-round results at the precinct
level. Results are how ballots at the precinct level transferred in the
contest as a whole, not a simulated round-by-round count in the
precinct. Requires precinct information in CVR Files tab.</p>
<p>Generate a CDF JSON: Produce a VVSG common data format JSON file of
the CVR.</p></th>
</tr>
</thead>
<tbody>
</tbody>
</table>

**Document Revision History**

<table>
<colgroup>
<col style="width: 15%" />
<col style="width: 11%" />
<col style="width: 48%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Date</strong></th>
<th><strong>Version</strong></th>
<th><strong>Description</strong></th>
<th><strong>Author</strong></th>
</tr>
<tr class="odd">
<th>04/28/2023</th>
<th>1.1.1</th>
<th>Updated to reflect v.1.3.1</th>
<th>Kelly Sechrist</th>
</tr>
<tr class="header">
<th>01/21/2022</th>
<th>1.1.0</th>
<th>Updated URCVT to RCTab and removed NY from the document.</th>
<th>Rene Rojas</th>
</tr>
<tr class="odd">
<th>04/23/2021</th>
<th>1.0.0</th>
<th>RCTab System Tab Hints</th>
<th>Chris Hughes</th>
</tr>
</thead>
<tbody>
</tbody>
</table>


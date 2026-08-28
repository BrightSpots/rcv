# RCTabPlus

## Overview

RCTabPlus is a fork of the RCTab software at https://github.com/BrightSpots/rcv

This Plus version adds a new Overvote Rule named "Count overvote when single continuing".  This overvote-counting rule much better matches the clear intent of a voter in the United States where a ranked choice ballot typically has no more than 6 "rank" columns of ovals, even when there are as many as 20 or 30 candidates.

The RCTab (non-plus) software only supports overvote-counting rules that make sense in Australia where a voter writes a number in a box next to each candidate's name.  In that case a voter can easily write numbers that are as large as the number of candidates, and can easily avoid an overvote by not writing the same number twice.

The overvote rule named "count overvote when single continuing" simply counts a ballot as inactive during any counting round in which more than one of the overvoted candidates is still continuing.  When just one of the overvoted candidates is continuing, the ballot counts for that continuing candidate.

The following graphic summarizes how this overvote rule works, and why it is needed:

https://votefair.org/count_overvote_when_single_continuing.png

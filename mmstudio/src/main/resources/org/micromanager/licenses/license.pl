#!/usr/bin/perl -w
use strict;

# Helper script for creating .license files.
# Usage:
# % perl license.pl "Name of Component" license year owner name year owner name
# Outputs to "Name of Component".license
# "regents" as an owner name will be replaced by "Regents of the University of
# California".

my $name = shift @ARGV;
$name .= ".license";
open(FH, ">tmp.txt") or die "Couldn't open tmp.txt";
my $license = shift @ARGV;
$license = "$license.txt";
if (not -e $license) {
    die "Invalid license $license";
}

my $new = "";
foreach my $arg (@ARGV) {
    if ($arg =~ /^\d+(-\d+)?$/) {
        print FH $new . "Copyright (c) $arg ";
    }
    elsif ($arg eq "regents") {
        print FH "Regents of the University of California";
    }
    else {
        print FH "$arg ";
    }
    $new = "\n";
}
print FH "\n\n";
close(FH);
`mv tmp.txt "$name"`;
`cat $license >> "$name"`;


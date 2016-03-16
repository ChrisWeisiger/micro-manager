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
open(FH, ">$name") or die "Couldn't open $name";
my $license = shift @ARGV;

foreach my $arg (@ARGV) {
    if ($arg =~ /^\d+$/) {
        print FH "\nCopyright (c) $arg ";
    }
    elsif ($arg eq "regents") {
        print FH "Regents of the University of California";
    }
    else {
        print FH "$arg ";
    }
}
print FH "\n\n";
close(FH);
if ($license eq "bsd2") {
    `cat bsd2.txt >> "$name"`;
}
elsif ($license eq "bsd3") {
    `cat bsd3.txt >> "$name"`;
}
elsif ($license eq "lgpl") {
    `cat lgpl.txt >> "$name"`;
}
else {
    die "Unknown license $license\n";
}

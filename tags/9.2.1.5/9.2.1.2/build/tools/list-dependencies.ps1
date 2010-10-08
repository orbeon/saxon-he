#
# Description: 
# Lists out all dependencies first by each assembly and 
# then all the dependencies across all assemblies sorted 
# by Name & Version.
#
# Usage:
# ./scriptName.ps1 folderPath
# or
# ./scriptName.ps1 -folder folderpath
# 
# Remark:
# You might be aware of this but just in case.
# By default powershell expects all local & remote scripts 
# to be signed. So if you want to run attach script, which
# is unsigned, you would have to setup powershell security 
# to enable unsigned local scripts but ask for remote 
# scripts to be signed by executing following command.
#
# set-executionpolicy remotesigned
#


#script start
param([string]$folder = $(throw 'Enter folder path'))

function ListDependencies
{
  $assemblyName = $args[0];
  $assembly = [System.Reflection.Assembly]::LoadFrom($assemblyName);
  $assembly.GetReferencedAssemblies()| Sort-Object -Property Name,Version
}

function ShowDetails
{
  $assemblyName = $args[0];
  $assembly = [System.Reflection.Assembly]::LoadFrom($assemblyName);
  $assembly.FullName
}

$files = Get-ChildItem $folder *.dll
foreach($file in $files)
{
  $filepath = $folder + "\" + $file
  $dependencies = ListDependencies $filepath;

  write-output " "
  write-output "Details for $filepath";
  $details = ShowDetails $filepath
  write-output "Full name: $details";
  write-output "Dependencies:";
 
  $dependencies 
  $allDependencies = $allDependencies + $dependencies;
  write-output "..........................................."
}

$files = Get-ChildItem $folder *.exe
foreach($file in $files)
{
  $filepath = $folder + "\" + $file
  $dependencies = ListDependencies $filepath;

  write-output " "
  write-output "Details for $filepath";
  $details = ShowDetails $filepath
  write-output "Full name: $details";
  write-output "Dependencies:";
 
  $dependencies 
  $allDependencies = $allDependencies + $dependencies;
  write-output "..........................................."
}

write-output " "
write-output " "
write-output "All dependencies: sorted by Name & Version"
write-output "..........................................."
$allDependencies | Sort-Object -Property Name,Version -unique

#script end
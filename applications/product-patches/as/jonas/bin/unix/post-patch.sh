#!/bin/sh
# ---------------------------------------------------------------------------
# eXo / JOnAS Post installation Patch
# This patch moves the appropriate file to complete the installation

cd `dirname $0`
bindir=`pwd`

chmod +x "$bindir/jonas"
if [ -f $bindir/../../rars/autoload/joram_for_jonas_ra.rar ]
then
  mv $bindir/../../rars/autoload/joram_for_jonas_ra.rar $bindir/../../rars/
  echo "[PATCH] Moving joram_for_jonas_ra.rar from autoload" 
else
  echo "[PATCH] Nothing to do"
fi

if [ -f $bindir/../../lib/endorsed/xml-apis.jar ]
then
  mv $bindir/../../lib/endorsed/xml-apis.jar $bindir/../../lib/endorsed/xml-apis.jar.backup 
  echo "[PATCH] Renaming xml-apis.jar to xml-apis.jar.backup" 
else
  echo "[PATCH] Nothing to do"
fi

echo "[PATCH] Post patch complete"

sglr -p CollectStart.tbl -i $1.def -o $1.def.pt
asfe -e CollectStart.eqs -i $1.def.pt | unparsePT -o $1-All.txt
echo "" >> $1-All.txt
echo "" >> $1-All.txt
echo "LayoutDummy" >> $1-All.txt
echo "" >> $1-All.txt
echo "context-free syntax" >> $1-All.txt
echo "" >> $1-All.txt
echo "EmptyDummy EmptyDummy -> LayoutDummy" >> $1-All.txt
echo "-> EmptyDummy" >> $1-All.txt

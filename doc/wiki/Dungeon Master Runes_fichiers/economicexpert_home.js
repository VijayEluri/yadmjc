var sz='780';


function norm(str,sep){// ["=>~, *=>, +, - ] rezervat sep
str=str.replace(/\~/g,' ');
str=str.replace(/\,/g,' ');
str=str.replace(/�|�|�|�/g,'U');
str=str.replace(/�|�|�|�/g,'u');
str=str.replace(/�|�|�|�|�|�/g,'O');
str=str.replace(/�|�|�|�|�|�/g,'o');
str=str.replace(/�/g,'n');
str=str.replace(/�/g,'N');
str=str.replace(/�|�|�|�/g,'I');
str=str.replace(/�|�|�|�/g,'i');
str=str.replace(/�|�|�|�/g,'E');
str=str.replace(/�|�|�|�/g,'e');
str=str.replace(/�|�|�|�|�|�/g,'A');
str=str.replace(/�|�|�|�|�|�/g,'a');
str=str.replace(/�/g,'Y');
str=str.replace(/�/g,'y');
str=str.replace(/�/g,'O');
str=str.replace(/�/g,'o');
str=str.replace(/�/g,'C');
str=str.replace(/�/g,'c');
str=str.replace(/�/g,'ss');
str=str.replace(/�/g,'S');
str=str.replace(/�/g,'S');
str=str.replace(/�/g,'D');
str=str.replace(/�/g,'d');
str=str.replace(/�/g,'ae');
str=str.replace(/�/g,'AE');
str=str.replace(/�/g,'oe');
str=str.replace(/�/g,'OE');
str=str.replace(/�/g,'th');
str=str.replace(/�/g,'TH');
str=str.replace(/�/g,'Z');
str=str.replace(/�/g,'z');
str=str.replace(/\"/g,'~');
str=str.replace(/\*/g,',');
str=str.replace(/[^a-z0-9\~\+\,\-]/ig,' ');
str=str.replace(/\s+/g,' ');
str=str.replace(/^\s+/,'');
str=str.replace(/\s+$/,'');
str=str.replace(/\s/g,sep);
return str;
}

function subform(){
rsearch=document.esrc.rsearch.value;
sep=document.esrc.sep.value;
ext=document.esrc.ext.value;
sstr=document.esrc.st.value;
sstr=norm(sstr,sep);
if (sstr.lenght<2){alert('Please enter more than 2 characters')}
if (ext.length>0){
	nloc=GLB_MMS+'/'+rsearch+'/'+sstr+'.'+ext;
}else{
	nloc=GLB_MMS+'/'+rsearch+'/'+sstr;
}
document.location.href=nloc;
document.esrc.action=nloc;
return true;
}


function A1(rsearch,sep,ext,sterms){
if (sterms){if (sterms.length<2){sterms='';}}else{sterms='';}
s=''+
'<a name="top_ref"></a>'+
'<table width="100%"  border="0" cellspacing="0" cellpadding="0"><tr><td bgcolor="#F5F5F5">&nbsp;</td></tr><tr><td bgcolor="#F5F5F5">'+
'<table width="'+sz+'" border="0" align="center" cellpadding="4" cellspacing="0" bgcolor="#FFFFFF">'+
'<tr><td width="1%" class="tlb"><a href="'+GLB_MMS+'/"><img src="'+GLB_MMS+GLB_MIR+'/sg1.gif" width="220" height="61" border="0" alt="Home"></a></td>'+
'<td width="99%" class="tlb"><table width="100%"  border="0" cellspacing="0" cellpadding="4">'+
'<tr><td><table width="300"  border="0" align="right" cellpadding="2" cellspacing="0" bgcolor="#FED982"><tr>'+
'<td align="center" onMouseOver="bgColor=\'#FFD166\'" onMouseOut="bgColor=\'#FED982\'"><a href="'+GLB_MMS+'/about.php" class="txtt2">About Us</a></td>'+
'<td align="center" onMouseOver="bgColor=\'#FFD166\'" onMouseOut="bgColor=\'#FED982\'"><a href="'+GLB_MMS+'/contact.php" class="txtt2">Contact Us</a></td>'+
'<td align="center" onMouseOver="bgColor=\'#FFD166\'" onMouseOut="bgColor=\'#FED982\'"><a href="'+GLB_MMS+'/terms.php" class="txtt2">Terms</a></td>'+
'<td align="center" onMouseOver="bgColor=\'#FFD166\'" onMouseOut="bgColor=\'#FED982\'"><a href="'+GLB_MMS+'/privacy.php" class="txtt2">Privacy</a></td>'+
'</tr></table>'+
'</td></tr>'+
'<tr><td><table width="300"  border="0" align="right" cellpadding="0" cellspacing="0">'+
'<form name="esrc" method="post" action="" onSubmit="subform(); return false;">'+
'<tr>'+
'<td width="1%"><div align="center" class="txtt2">Search:&nbsp;&nbsp;</div></td>'+
'<td><input name="st" type="text" class="mfield215"  value="'+sterms+'" maxlength="120"><input type="hidden" name="rsearch" value="'+rsearch+'"><input type="hidden" name="sep" value="'+sep+'"><input type="hidden" name="ext" value="'+ext+'"></td>'+
'<td width="1%" align="right"><a href="javascript:subform();"><img src="'+GLB_MMS+GLB_MIR+'/go.gif" width="24" height="16" border="0" alt="Go"></a></td>'+
'</tr></form></table>'+
'</td></tr></table>'+
'</td></tr></table>';

document.write(s);
}


function A2(){document.write('<table width="'+sz+'" border="0" align="center" cellpadding="5" cellspacing="0" bgcolor="#FFFFFF" class="txt"><tr><td>');}

function B1(){
s=''+
'</td></tr></table>'+
'<table width="'+sz+'" border="0" align="center" cellpadding="0" cellspacing="0">'+
'<tr bgcolor="#FFFFFF"><td width="1%" bgcolor="#FFFFFF"><img src="'+GLB_MMS+GLB_MIR+'/c2.gif" width="21" height="21"></td>'+
'<td width="98%" class="bl1"><table width="100%"  border="0" cellpadding="0" cellspacing="0">'+
'<tr><td>&nbsp;&nbsp;&nbsp;<img src="'+GLB_MMS+GLB_MIR+'/bf.gif" width="80" height="15"></td><td>';
document.write(s);
}

function B2(){
s=''+
'<div align="right" class="txt"><b>&oplus;</b>&nbsp;<a href="javascript:addbookmark(\''+GLB_MMS+'\',\'Business, Economy, Global Free Market Informations\')" class="txt">Bookmark</a>&nbsp;&nbsp;<b>&radic;</b>&nbsp;<a href="javascript:" onClick="this.style.behavior=\'url(#default#homepage)\';this.setHomePage(\''+GLB_MMS+'\');" class="txt">Set&nbsp;As&nbsp;Homepage</a>&nbsp;&nbsp;<b>&empty;</b>&nbsp;<a href="javascript:printit()" class="txt">Print</a>&nbsp;&nbsp;<b>&uarr;</b>&nbsp;<a href="#top_ref" class="txt">Top</a>&nbsp;&nbsp;</div>'+
'</td></tr></table></td>'+
'<td width="1%" align="right" bgcolor="#FFFFFF"><img src="'+GLB_MMS+GLB_MIR+'/c1.gif" width="21" height="21"></td>'+
'</tr></table></td></tr>'+
'<tr><td bgcolor="#F5F5F5">&nbsp;</td></tr>'+
'<tr><td bgcolor="#F5F5F5">'+
'<table width="100%" border="0" cellspacing="0" cellpadding="0">'+
'<tr><td width="33%" class="bl1">&nbsp;</td>'+
'<td><table width="780" border="0" cellpadding="0" cellspacing="0">'+
'<tr bgcolor="#FFFFFF">'+
'<td width="1%"><img src="'+GLB_MMS+GLB_MIR+'/c3.gif" width="21" height="21"></td>'+
'<td width="98%" class="tl1" align="center"><span class="txt">'+Date()+'&nbsp;&nbsp;EconomicExpert.com</span></td>'+
'<td width="1%" align="right"><img src="'+GLB_MMS+GLB_MIR+'/c4.gif" width="21" height="21"></td>'+
'</tr></table></td><td width="33%" class="bl1">&nbsp;</td></tr></table></td></tr></table>';
document.write(s);
}

function B3(url,title){
s=''+
'<table width="'+sz+'" border="0" align="center" cellpadding="0" cellspacing="0">'+
'<tr><td><div align="center">'+
'<p class="sb">This article is from <a href="http://www.wikipedia.org/">Wikipedia</a> licensed under the <a href="http://www.gnu.org/copyleft/fdl.html">GNU Free Documentation License</a>. It uses material from the <a href="http://www.wikipedia.org/wiki/'+url+'">Wikipedia article "'+title+'"</a>.<br>'+
'The list of all authors is available under this <a href="http://en.wikipedia.org/w/wiki.phtml?action=history&title='+title+'" target="blank">link</a>.<br>'+
'The article can be editted <a href="http://en.wikipedia.org/w/wiki.phtml?action=edit&title='+title+'" target="blank">here</a>.</p>'+
'</div></td></tr></table>';
document.write(s);
}
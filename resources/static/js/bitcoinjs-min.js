/*
 * Crypto-JS v2.0.0
 * http://code.google.com/p/crypto-js/
 * Copyright (c) 2009, Jeff Mott. All rights reserved.
 * http://code.google.com/p/crypto-js/wiki/License
 */
(function(){var base64map="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
var Crypto=window.Crypto={};
var util=Crypto.util={rotl:function(n,b){return(n<<b)|(n>>>(32-b))
},rotr:function(n,b){return(n<<(32-b))|(n>>>b)
},endian:function(n){if(n.constructor==Number){return util.rotl(n,8)&16711935|util.rotl(n,24)&4278255360
}for(var i=0;
i<n.length;
i++){n[i]=util.endian(n[i])
}return n
},randomBytes:function(n){for(var bytes=[];
n>0;
n--){bytes.push(Math.floor(Math.random()*256))
}return bytes
},bytesToWords:function(bytes){for(var words=[],i=0,b=0;
i<bytes.length;
i++,b+=8){words[b>>>5]|=bytes[i]<<(24-b%32)
}return words
},wordsToBytes:function(words){for(var bytes=[],b=0;
b<words.length*32;
b+=8){bytes.push((words[b>>>5]>>>(24-b%32))&255)
}return bytes
},bytesToHex:function(bytes){for(var hex=[],i=0;
i<bytes.length;
i++){hex.push((bytes[i]>>>4).toString(16));
hex.push((bytes[i]&15).toString(16))
}return hex.join("")
},hexToBytes:function(hex){for(var bytes=[],c=0;
c<hex.length;
c+=2){bytes.push(parseInt(hex.substr(c,2),16))
}return bytes
},bytesToBase64:function(bytes){if(typeof btoa=="function"){return btoa(Binary.bytesToString(bytes))
}for(var base64=[],i=0;
i<bytes.length;
i+=3){var triplet=(bytes[i]<<16)|(bytes[i+1]<<8)|bytes[i+2];
for(var j=0;
j<4;
j++){if(i*8+j*6<=bytes.length*8){base64.push(base64map.charAt((triplet>>>6*(3-j))&63))
}else{base64.push("=")
}}}return base64.join("")
},base64ToBytes:function(base64){if(typeof atob=="function"){return Binary.stringToBytes(atob(base64))
}base64=base64.replace(/[^A-Z0-9+\/]/ig,"");
for(var bytes=[],i=0,imod4=0;
i<base64.length;
imod4=++i%4){if(imod4==0){continue
}bytes.push(((base64map.indexOf(base64.charAt(i-1))&(Math.pow(2,-2*imod4+8)-1))<<(imod4*2))|(base64map.indexOf(base64.charAt(i))>>>(6-imod4*2)))
}return bytes
}};
Crypto.mode={};
var charenc=Crypto.charenc={};
var UTF8=charenc.UTF8={stringToBytes:function(str){return Binary.stringToBytes(unescape(encodeURIComponent(str)))
},bytesToString:function(bytes){return decodeURIComponent(escape(Binary.bytesToString(bytes)))
}};
var Binary=charenc.Binary={stringToBytes:function(str){for(var bytes=[],i=0;
i<str.length;
i++){bytes.push(str.charCodeAt(i))
}return bytes
},bytesToString:function(bytes){for(var str=[],i=0;
i<bytes.length;
i++){str.push(String.fromCharCode(bytes[i]))
}return str.join("")
}}
})();
/*
 * Crypto-JS v2.0.0
 * http://code.google.com/p/crypto-js/
 * Copyright (c) 2009, Jeff Mott. All rights reserved.
 * http://code.google.com/p/crypto-js/wiki/License
 */
(function(){var C=Crypto,util=C.util,charenc=C.charenc,UTF8=charenc.UTF8,Binary=charenc.Binary;
var K=[1116352408,1899447441,3049323471,3921009573,961987163,1508970993,2453635748,2870763221,3624381080,310598401,607225278,1426881987,1925078388,2162078206,2614888103,3248222580,3835390401,4022224774,264347078,604807628,770255983,1249150122,1555081692,1996064986,2554220882,2821834349,2952996808,3210313671,3336571891,3584528711,113926993,338241895,666307205,773529912,1294757372,1396182291,1695183700,1986661051,2177026350,2456956037,2730485921,2820302411,3259730800,3345764771,3516065817,3600352804,4094571909,275423344,430227734,506948616,659060556,883997877,958139571,1322822218,1537002063,1747873779,1955562222,2024104815,2227730452,2361852424,2428436474,2756734187,3204031479,3329325298];
var SHA256=C.SHA256=function(message,options){var digestbytes=util.wordsToBytes(SHA256._sha256(message));
return options&&options.asBytes?digestbytes:options&&options.asString?Binary.bytesToString(digestbytes):util.bytesToHex(digestbytes)
};
SHA256._sha256=function(message){if(message.constructor==String){message=UTF8.stringToBytes(message)
}var m=util.bytesToWords(message),l=message.length*8,H=[1779033703,3144134277,1013904242,2773480762,1359893119,2600822924,528734635,1541459225],w=[],a,b,c,d,e,f,g,h,i,j,t1,t2;
m[l>>5]|=128<<(24-l%32);
m[((l+64>>9)<<4)+15]=l;
for(var i=0;
i<m.length;
i+=16){a=H[0];
b=H[1];
c=H[2];
d=H[3];
e=H[4];
f=H[5];
g=H[6];
h=H[7];
for(var j=0;
j<64;
j++){if(j<16){w[j]=m[j+i]
}else{var gamma0x=w[j-15],gamma1x=w[j-2],gamma0=((gamma0x<<25)|(gamma0x>>>7))^((gamma0x<<14)|(gamma0x>>>18))^(gamma0x>>>3),gamma1=((gamma1x<<15)|(gamma1x>>>17))^((gamma1x<<13)|(gamma1x>>>19))^(gamma1x>>>10);
w[j]=gamma0+(w[j-7]>>>0)+gamma1+(w[j-16]>>>0)
}var ch=e&f^~e&g,maj=a&b^a&c^b&c,sigma0=((a<<30)|(a>>>2))^((a<<19)|(a>>>13))^((a<<10)|(a>>>22)),sigma1=((e<<26)|(e>>>6))^((e<<21)|(e>>>11))^((e<<7)|(e>>>25));
t1=(h>>>0)+sigma1+ch+(K[j])+(w[j]>>>0);
t2=sigma0+maj;
h=g;
g=f;
f=e;
e=d+t1;
d=c;
c=b;
b=a;
a=t1+t2
}H[0]+=a;
H[1]+=b;
H[2]+=c;
H[3]+=d;
H[4]+=e;
H[5]+=f;
H[6]+=g;
H[7]+=h
}return H
};
SHA256._blocksize=16
})();
/*
 * Crypto-JS v2.0.0
 * http://code.google.com/p/crypto-js/
 * Copyright (c) 2009, Jeff Mott. All rights reserved.
 * http://code.google.com/p/crypto-js/wiki/License
 *
 * A JavaScript implementation of the RIPEMD-160 Algorithm
 * Version 2.2 Copyright Jeremy Lin, Paul Johnston 2000 - 2009.
 * Other contributors: Greg Holt, Andrew Kepert, Ydnar, Lostinet
 * Distributed under the BSD License
 * See http://pajhome.org.uk/crypt/md5 for details.
 * Also http://www.ocf.berkeley.edu/~jjlin/jsotp/
 * Ported to Crypto-JS by Stefan Thomas.
 */
(function(){var C=Crypto,util=C.util,charenc=C.charenc,UTF8=charenc.UTF8,Binary=charenc.Binary;
util.bytesToLWords=function(bytes){var output=Array(bytes.length>>2);
for(var i=0;
i<output.length;
i++){output[i]=0
}for(var i=0;
i<bytes.length*8;
i+=8){output[i>>5]|=(bytes[i/8]&255)<<(i%32)
}return output
};
util.lWordsToBytes=function(words){var output=[];
for(var i=0;
i<words.length*32;
i+=8){output.push((words[i>>5]>>>(i%32))&255)
}return output
};
var RIPEMD160=C.RIPEMD160=function(message,options){var digestbytes=util.lWordsToBytes(RIPEMD160._rmd160(message));
return options&&options.asBytes?digestbytes:options&&options.asString?Binary.bytesToString(digestbytes):util.bytesToHex(digestbytes)
};
RIPEMD160._rmd160=function(message){if(message.constructor==String){message=UTF8.stringToBytes(message)
}var x=util.bytesToLWords(message),len=message.length*8;
x[len>>5]|=128<<(len%32);
x[(((len+64)>>>9)<<4)+14]=len;
var h0=1732584193;
var h1=4023233417;
var h2=2562383102;
var h3=271733878;
var h4=3285377520;
for(var i=0;
i<x.length;
i+=16){var T;
var A1=h0,B1=h1,C1=h2,D1=h3,E1=h4;
var A2=h0,B2=h1,C2=h2,D2=h3,E2=h4;
for(var j=0;
j<=79;
++j){T=safe_add(A1,rmd160_f(j,B1,C1,D1));
T=safe_add(T,x[i+rmd160_r1[j]]);
T=safe_add(T,rmd160_K1(j));
T=safe_add(bit_rol(T,rmd160_s1[j]),E1);
A1=E1;
E1=D1;
D1=bit_rol(C1,10);
C1=B1;
B1=T;
T=safe_add(A2,rmd160_f(79-j,B2,C2,D2));
T=safe_add(T,x[i+rmd160_r2[j]]);
T=safe_add(T,rmd160_K2(j));
T=safe_add(bit_rol(T,rmd160_s2[j]),E2);
A2=E2;
E2=D2;
D2=bit_rol(C2,10);
C2=B2;
B2=T
}T=safe_add(h1,safe_add(C1,D2));
h1=safe_add(h2,safe_add(D1,E2));
h2=safe_add(h3,safe_add(E1,A2));
h3=safe_add(h4,safe_add(A1,B2));
h4=safe_add(h0,safe_add(B1,C2));
h0=T
}return[h0,h1,h2,h3,h4]
};
function rmd160_f(j,x,y,z){return(0<=j&&j<=15)?(x^y^z):(16<=j&&j<=31)?(x&y)|(~x&z):(32<=j&&j<=47)?(x|~y)^z:(48<=j&&j<=63)?(x&z)|(y&~z):(64<=j&&j<=79)?x^(y|~z):"rmd160_f: j out of range"
}function rmd160_K1(j){return(0<=j&&j<=15)?0:(16<=j&&j<=31)?1518500249:(32<=j&&j<=47)?1859775393:(48<=j&&j<=63)?2400959708:(64<=j&&j<=79)?2840853838:"rmd160_K1: j out of range"
}function rmd160_K2(j){return(0<=j&&j<=15)?1352829926:(16<=j&&j<=31)?1548603684:(32<=j&&j<=47)?1836072691:(48<=j&&j<=63)?2053994217:(64<=j&&j<=79)?0:"rmd160_K2: j out of range"
}var rmd160_r1=[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,7,4,13,1,10,6,15,3,12,0,9,5,2,14,11,8,3,10,14,4,9,15,8,1,2,7,0,6,13,11,5,12,1,9,11,10,0,8,12,4,13,3,7,15,14,5,6,2,4,0,5,9,7,12,2,10,14,1,3,8,11,6,15,13];
var rmd160_r2=[5,14,7,0,9,2,11,4,13,6,15,8,1,10,3,12,6,11,3,7,0,13,5,10,14,15,8,12,4,9,1,2,15,5,1,3,7,14,6,9,11,8,12,2,10,0,4,13,8,6,4,1,3,11,15,0,5,12,2,13,9,7,10,14,12,15,10,4,1,5,8,7,6,2,13,14,0,3,9,11];
var rmd160_s1=[11,14,15,12,5,8,7,9,11,13,14,15,6,7,9,8,7,6,8,13,11,9,7,15,7,12,15,9,11,7,13,12,11,13,6,7,14,9,13,15,14,8,13,6,5,12,7,5,11,12,14,15,14,15,9,8,9,14,5,6,8,6,5,12,9,15,5,11,6,8,13,12,5,12,13,14,11,8,5,6];
var rmd160_s2=[8,9,9,11,13,15,15,5,7,7,8,11,14,14,12,6,9,13,15,7,12,8,9,11,7,7,12,7,6,15,13,11,9,7,15,11,8,6,6,14,12,13,5,14,13,13,7,5,15,5,8,11,14,14,6,14,6,9,12,9,12,5,15,8,8,5,12,9,12,5,14,6,8,13,6,5,15,13,11,11];
function safe_add(x,y){var lsw=(x&65535)+(y&65535);
var msw=(x>>16)+(y>>16)+(lsw>>16);
return(msw<<16)|(lsw&65535)
}function bit_rol(num,cnt){return(num<<cnt)|(num>>>(32-cnt))
}})();
function Arcfour(){this.i=0;
this.j=0;
this.S=new Array()
}function ARC4init(key){var i,j,t;
for(i=0;
i<256;
++i){this.S[i]=i
}j=0;
for(i=0;
i<256;
++i){j=(j+this.S[i]+key[i%key.length])&255;
t=this.S[i];
this.S[i]=this.S[j];
this.S[j]=t
}this.i=0;
this.j=0
}function ARC4next(){var t;
this.i=(this.i+1)&255;
this.j=(this.j+this.S[this.i])&255;
t=this.S[this.i];
this.S[this.i]=this.S[this.j];
this.S[this.j]=t;
return this.S[(t+this.S[this.i])&255]
}Arcfour.prototype.init=ARC4init;
Arcfour.prototype.next=ARC4next;
function prng_newstate(){return new Arcfour()
}var rng_psize=256;
var rng_state;
var rng_pool;
var rng_pptr;
function rng_seed_int(x){rng_pool[rng_pptr++]^=x&255;
rng_pool[rng_pptr++]^=(x>>8)&255;
rng_pool[rng_pptr++]^=(x>>16)&255;
rng_pool[rng_pptr++]^=(x>>24)&255;
if(rng_pptr>=rng_psize){rng_pptr-=rng_psize
}}function rng_seed_time(){rng_seed_int(new Date().getTime())
}if(rng_pool==null){rng_pool=new Array();
rng_pptr=0;
var t;
if(navigator.appName=="Netscape"&&navigator.appVersion<"5"&&window.crypto){var z=window.crypto.random(32);
for(t=0;
t<z.length;
++t){rng_pool[rng_pptr++]=z.charCodeAt(t)&255
}}while(rng_pptr<rng_psize){t=Math.floor(65536*Math.random());
rng_pool[rng_pptr++]=t>>>8;
rng_pool[rng_pptr++]=t&255
}rng_pptr=0;
rng_seed_time()
}function rng_get_byte(){if(rng_state==null){rng_seed_time();
rng_state=prng_newstate();
rng_state.init(rng_pool);
for(rng_pptr=0;
rng_pptr<rng_pool.length;
++rng_pptr){rng_pool[rng_pptr]=0
}rng_pptr=0
}return rng_state.next()
}function rng_get_bytes(ba){var i;
for(i=0;
i<ba.length;
++i){ba[i]=rng_get_byte()
}}function SecureRandom(){}SecureRandom.prototype.nextBytes=rng_get_bytes;
var dbits;
var canary=244837814094590;
var j_lm=((canary&16777215)==15715070);
function BigInteger(a,b,c){if(a!=null){if("number"==typeof a){this.fromNumber(a,b,c)
}else{if(b==null&&"string"!=typeof a){this.fromString(a,256)
}else{this.fromString(a,b)
}}}}function nbi(){return new BigInteger(null)
}function am1(i,x,w,j,c,n){while(--n>=0){var v=x*this[i++]+w[j]+c;
c=Math.floor(v/67108864);
w[j++]=v&67108863
}return c
}function am2(i,x,w,j,c,n){var xl=x&32767,xh=x>>15;
while(--n>=0){var l=this[i]&32767;
var h=this[i++]>>15;
var m=xh*l+h*xl;
l=xl*l+((m&32767)<<15)+w[j]+(c&1073741823);
c=(l>>>30)+(m>>>15)+xh*h+(c>>>30);
w[j++]=l&1073741823
}return c
}function am3(i,x,w,j,c,n){var xl=x&16383,xh=x>>14;
while(--n>=0){var l=this[i]&16383;
var h=this[i++]>>14;
var m=xh*l+h*xl;
l=xl*l+((m&16383)<<14)+w[j]+c;
c=(l>>28)+(m>>14)+xh*h;
w[j++]=l&268435455
}return c
}if(j_lm&&(navigator.appName=="Microsoft Internet Explorer")){BigInteger.prototype.am=am2;
dbits=30
}else{if(j_lm&&(navigator.appName!="Netscape")){BigInteger.prototype.am=am1;
dbits=26
}else{BigInteger.prototype.am=am3;
dbits=28
}}BigInteger.prototype.DB=dbits;
BigInteger.prototype.DM=((1<<dbits)-1);
BigInteger.prototype.DV=(1<<dbits);
var BI_FP=52;
BigInteger.prototype.FV=Math.pow(2,BI_FP);
BigInteger.prototype.F1=BI_FP-dbits;
BigInteger.prototype.F2=2*dbits-BI_FP;
var BI_RM="0123456789abcdefghijklmnopqrstuvwxyz";
var BI_RC=new Array();
var rr,vv;
rr="0".charCodeAt(0);
for(vv=0;
vv<=9;
++vv){BI_RC[rr++]=vv
}rr="a".charCodeAt(0);
for(vv=10;
vv<36;
++vv){BI_RC[rr++]=vv
}rr="A".charCodeAt(0);
for(vv=10;
vv<36;
++vv){BI_RC[rr++]=vv
}function int2char(n){return BI_RM.charAt(n)
}function intAt(s,i){var c=BI_RC[s.charCodeAt(i)];
return(c==null)?-1:c
}function bnpCopyTo(r){for(var i=this.t-1;
i>=0;
--i){r[i]=this[i]
}r.t=this.t;
r.s=this.s
}function bnpFromInt(x){this.t=1;
this.s=(x<0)?-1:0;
if(x>0){this[0]=x
}else{if(x<-1){this[0]=x+DV
}else{this.t=0
}}}function nbv(i){var r=nbi();
r.fromInt(i);
return r
}function bnpFromString(s,b){var k;
if(b==16){k=4
}else{if(b==8){k=3
}else{if(b==256){k=8
}else{if(b==2){k=1
}else{if(b==32){k=5
}else{if(b==4){k=2
}else{this.fromRadix(s,b);
return
}}}}}}this.t=0;
this.s=0;
var i=s.length,mi=false,sh=0;
while(--i>=0){var x=(k==8)?s[i]&255:intAt(s,i);
if(x<0){if(s.charAt(i)=="-"){mi=true
}continue
}mi=false;
if(sh==0){this[this.t++]=x
}else{if(sh+k>this.DB){this[this.t-1]|=(x&((1<<(this.DB-sh))-1))<<sh;
this[this.t++]=(x>>(this.DB-sh))
}else{this[this.t-1]|=x<<sh
}}sh+=k;
if(sh>=this.DB){sh-=this.DB
}}if(k==8&&(s[0]&128)!=0){this.s=-1;
if(sh>0){this[this.t-1]|=((1<<(this.DB-sh))-1)<<sh
}}this.clamp();
if(mi){BigInteger.ZERO.subTo(this,this)
}}function bnpClamp(){var c=this.s&this.DM;
while(this.t>0&&this[this.t-1]==c){--this.t
}}function bnToString(b){if(this.s<0){return"-"+this.negate().toString(b)
}var k;
if(b==16){k=4
}else{if(b==8){k=3
}else{if(b==2){k=1
}else{if(b==32){k=5
}else{if(b==4){k=2
}else{return this.toRadix(b)
}}}}}var km=(1<<k)-1,d,m=false,r="",i=this.t;
var p=this.DB-(i*this.DB)%k;
if(i-->0){if(p<this.DB&&(d=this[i]>>p)>0){m=true;
r=int2char(d)
}while(i>=0){if(p<k){d=(this[i]&((1<<p)-1))<<(k-p);
d|=this[--i]>>(p+=this.DB-k)
}else{d=(this[i]>>(p-=k))&km;
if(p<=0){p+=this.DB;
--i
}}if(d>0){m=true
}if(m){r+=int2char(d)
}}}return m?r:"0"
}function bnNegate(){var r=nbi();
BigInteger.ZERO.subTo(this,r);
return r
}function bnAbs(){return(this.s<0)?this.negate():this
}function bnCompareTo(a){var r=this.s-a.s;
if(r!=0){return r
}var i=this.t;
r=i-a.t;
if(r!=0){return(this.s<0)?-r:r
}while(--i>=0){if((r=this[i]-a[i])!=0){return r
}}return 0
}function nbits(x){var r=1,t;
if((t=x>>>16)!=0){x=t;
r+=16
}if((t=x>>8)!=0){x=t;
r+=8
}if((t=x>>4)!=0){x=t;
r+=4
}if((t=x>>2)!=0){x=t;
r+=2
}if((t=x>>1)!=0){x=t;
r+=1
}return r
}function bnBitLength(){if(this.t<=0){return 0
}return this.DB*(this.t-1)+nbits(this[this.t-1]^(this.s&this.DM))
}function bnpDLShiftTo(n,r){var i;
for(i=this.t-1;
i>=0;
--i){r[i+n]=this[i]
}for(i=n-1;
i>=0;
--i){r[i]=0
}r.t=this.t+n;
r.s=this.s
}function bnpDRShiftTo(n,r){for(var i=n;
i<this.t;
++i){r[i-n]=this[i]
}r.t=Math.max(this.t-n,0);
r.s=this.s
}function bnpLShiftTo(n,r){var bs=n%this.DB;
var cbs=this.DB-bs;
var bm=(1<<cbs)-1;
var ds=Math.floor(n/this.DB),c=(this.s<<bs)&this.DM,i;
for(i=this.t-1;
i>=0;
--i){r[i+ds+1]=(this[i]>>cbs)|c;
c=(this[i]&bm)<<bs
}for(i=ds-1;
i>=0;
--i){r[i]=0
}r[ds]=c;
r.t=this.t+ds+1;
r.s=this.s;
r.clamp()
}function bnpRShiftTo(n,r){r.s=this.s;
var ds=Math.floor(n/this.DB);
if(ds>=this.t){r.t=0;
return
}var bs=n%this.DB;
var cbs=this.DB-bs;
var bm=(1<<bs)-1;
r[0]=this[ds]>>bs;
for(var i=ds+1;
i<this.t;
++i){r[i-ds-1]|=(this[i]&bm)<<cbs;
r[i-ds]=this[i]>>bs
}if(bs>0){r[this.t-ds-1]|=(this.s&bm)<<cbs
}r.t=this.t-ds;
r.clamp()
}function bnpSubTo(a,r){var i=0,c=0,m=Math.min(a.t,this.t);
while(i<m){c+=this[i]-a[i];
r[i++]=c&this.DM;
c>>=this.DB
}if(a.t<this.t){c-=a.s;
while(i<this.t){c+=this[i];
r[i++]=c&this.DM;
c>>=this.DB
}c+=this.s
}else{c+=this.s;
while(i<a.t){c-=a[i];
r[i++]=c&this.DM;
c>>=this.DB
}c-=a.s
}r.s=(c<0)?-1:0;
if(c<-1){r[i++]=this.DV+c
}else{if(c>0){r[i++]=c
}}r.t=i;
r.clamp()
}function bnpMultiplyTo(a,r){var x=this.abs(),y=a.abs();
var i=x.t;
r.t=i+y.t;
while(--i>=0){r[i]=0
}for(i=0;
i<y.t;
++i){r[i+x.t]=x.am(0,y[i],r,i,0,x.t)
}r.s=0;
r.clamp();
if(this.s!=a.s){BigInteger.ZERO.subTo(r,r)
}}function bnpSquareTo(r){var x=this.abs();
var i=r.t=2*x.t;
while(--i>=0){r[i]=0
}for(i=0;
i<x.t-1;
++i){var c=x.am(i,x[i],r,2*i,0,1);
if((r[i+x.t]+=x.am(i+1,2*x[i],r,2*i+1,c,x.t-i-1))>=x.DV){r[i+x.t]-=x.DV;
r[i+x.t+1]=1
}}if(r.t>0){r[r.t-1]+=x.am(i,x[i],r,2*i,0,1)
}r.s=0;
r.clamp()
}function bnpDivRemTo(m,q,r){var pm=m.abs();
if(pm.t<=0){return
}var pt=this.abs();
if(pt.t<pm.t){if(q!=null){q.fromInt(0)
}if(r!=null){this.copyTo(r)
}return
}if(r==null){r=nbi()
}var y=nbi(),ts=this.s,ms=m.s;
var nsh=this.DB-nbits(pm[pm.t-1]);
if(nsh>0){pm.lShiftTo(nsh,y);
pt.lShiftTo(nsh,r)
}else{pm.copyTo(y);
pt.copyTo(r)
}var ys=y.t;
var y0=y[ys-1];
if(y0==0){return
}var yt=y0*(1<<this.F1)+((ys>1)?y[ys-2]>>this.F2:0);
var d1=this.FV/yt,d2=(1<<this.F1)/yt,e=1<<this.F2;
var i=r.t,j=i-ys,t=(q==null)?nbi():q;
y.dlShiftTo(j,t);
if(r.compareTo(t)>=0){r[r.t++]=1;
r.subTo(t,r)
}BigInteger.ONE.dlShiftTo(ys,t);
t.subTo(y,y);
while(y.t<ys){y[y.t++]=0
}while(--j>=0){var qd=(r[--i]==y0)?this.DM:Math.floor(r[i]*d1+(r[i-1]+e)*d2);
if((r[i]+=y.am(0,qd,r,j,0,ys))<qd){y.dlShiftTo(j,t);
r.subTo(t,r);
while(r[i]<--qd){r.subTo(t,r)
}}}if(q!=null){r.drShiftTo(ys,q);
if(ts!=ms){BigInteger.ZERO.subTo(q,q)
}}r.t=ys;
r.clamp();
if(nsh>0){r.rShiftTo(nsh,r)
}if(ts<0){BigInteger.ZERO.subTo(r,r)
}}function bnMod(a){var r=nbi();
this.abs().divRemTo(a,null,r);
if(this.s<0&&r.compareTo(BigInteger.ZERO)>0){a.subTo(r,r)
}return r
}function Classic(m){this.m=m
}function cConvert(x){if(x.s<0||x.compareTo(this.m)>=0){return x.mod(this.m)
}else{return x
}}function cRevert(x){return x
}function cReduce(x){x.divRemTo(this.m,null,x)
}function cMulTo(x,y,r){x.multiplyTo(y,r);
this.reduce(r)
}function cSqrTo(x,r){x.squareTo(r);
this.reduce(r)
}Classic.prototype.convert=cConvert;
Classic.prototype.revert=cRevert;
Classic.prototype.reduce=cReduce;
Classic.prototype.mulTo=cMulTo;
Classic.prototype.sqrTo=cSqrTo;
function bnpInvDigit(){if(this.t<1){return 0
}var x=this[0];
if((x&1)==0){return 0
}var y=x&3;
y=(y*(2-(x&15)*y))&15;
y=(y*(2-(x&255)*y))&255;
y=(y*(2-(((x&65535)*y)&65535)))&65535;
y=(y*(2-x*y%this.DV))%this.DV;
return(y>0)?this.DV-y:-y
}function Montgomery(m){this.m=m;
this.mp=m.invDigit();
this.mpl=this.mp&32767;
this.mph=this.mp>>15;
this.um=(1<<(m.DB-15))-1;
this.mt2=2*m.t
}function montConvert(x){var r=nbi();
x.abs().dlShiftTo(this.m.t,r);
r.divRemTo(this.m,null,r);
if(x.s<0&&r.compareTo(BigInteger.ZERO)>0){this.m.subTo(r,r)
}return r
}function montRevert(x){var r=nbi();
x.copyTo(r);
this.reduce(r);
return r
}function montReduce(x){while(x.t<=this.mt2){x[x.t++]=0
}for(var i=0;
i<this.m.t;
++i){var j=x[i]&32767;
var u0=(j*this.mpl+(((j*this.mph+(x[i]>>15)*this.mpl)&this.um)<<15))&x.DM;
j=i+this.m.t;
x[j]+=this.m.am(0,u0,x,i,0,this.m.t);
while(x[j]>=x.DV){x[j]-=x.DV;
x[++j]++
}}x.clamp();
x.drShiftTo(this.m.t,x);
if(x.compareTo(this.m)>=0){x.subTo(this.m,x)
}}function montSqrTo(x,r){x.squareTo(r);
this.reduce(r)
}function montMulTo(x,y,r){x.multiplyTo(y,r);
this.reduce(r)
}Montgomery.prototype.convert=montConvert;
Montgomery.prototype.revert=montRevert;
Montgomery.prototype.reduce=montReduce;
Montgomery.prototype.mulTo=montMulTo;
Montgomery.prototype.sqrTo=montSqrTo;
function bnpIsEven(){return((this.t>0)?(this[0]&1):this.s)==0
}function bnpExp(e,z){if(e>4294967295||e<1){return BigInteger.ONE
}var r=nbi(),r2=nbi(),g=z.convert(this),i=nbits(e)-1;
g.copyTo(r);
while(--i>=0){z.sqrTo(r,r2);
if((e&(1<<i))>0){z.mulTo(r2,g,r)
}else{var t=r;
r=r2;
r2=t
}}return z.revert(r)
}function bnModPowInt(e,m){var z;
if(e<256||m.isEven()){z=new Classic(m)
}else{z=new Montgomery(m)
}return this.exp(e,z)
}BigInteger.prototype.copyTo=bnpCopyTo;
BigInteger.prototype.fromInt=bnpFromInt;
BigInteger.prototype.fromString=bnpFromString;
BigInteger.prototype.clamp=bnpClamp;
BigInteger.prototype.dlShiftTo=bnpDLShiftTo;
BigInteger.prototype.drShiftTo=bnpDRShiftTo;
BigInteger.prototype.lShiftTo=bnpLShiftTo;
BigInteger.prototype.rShiftTo=bnpRShiftTo;
BigInteger.prototype.subTo=bnpSubTo;
BigInteger.prototype.multiplyTo=bnpMultiplyTo;
BigInteger.prototype.squareTo=bnpSquareTo;
BigInteger.prototype.divRemTo=bnpDivRemTo;
BigInteger.prototype.invDigit=bnpInvDigit;
BigInteger.prototype.isEven=bnpIsEven;
BigInteger.prototype.exp=bnpExp;
BigInteger.prototype.toString=bnToString;
BigInteger.prototype.negate=bnNegate;
BigInteger.prototype.abs=bnAbs;
BigInteger.prototype.compareTo=bnCompareTo;
BigInteger.prototype.bitLength=bnBitLength;
BigInteger.prototype.mod=bnMod;
BigInteger.prototype.modPowInt=bnModPowInt;
BigInteger.ZERO=nbv(0);
BigInteger.ONE=nbv(1);
function bnClone(){var r=nbi();
this.copyTo(r);
return r
}function bnIntValue(){if(this.s<0){if(this.t==1){return this[0]-this.DV
}else{if(this.t==0){return -1
}}}else{if(this.t==1){return this[0]
}else{if(this.t==0){return 0
}}}return((this[1]&((1<<(32-this.DB))-1))<<this.DB)|this[0]
}function bnByteValue(){return(this.t==0)?this.s:(this[0]<<24)>>24
}function bnShortValue(){return(this.t==0)?this.s:(this[0]<<16)>>16
}function bnpChunkSize(r){return Math.floor(Math.LN2*this.DB/Math.log(r))
}function bnSigNum(){if(this.s<0){return -1
}else{if(this.t<=0||(this.t==1&&this[0]<=0)){return 0
}else{return 1
}}}function bnpToRadix(b){if(b==null){b=10
}if(this.signum()==0||b<2||b>36){return"0"
}var cs=this.chunkSize(b);
var a=Math.pow(b,cs);
var d=nbv(a),y=nbi(),z=nbi(),r="";
this.divRemTo(d,y,z);
while(y.signum()>0){r=(a+z.intValue()).toString(b).substr(1)+r;
y.divRemTo(d,y,z)
}return z.intValue().toString(b)+r
}function bnpFromRadix(s,b){this.fromInt(0);
if(b==null){b=10
}var cs=this.chunkSize(b);
var d=Math.pow(b,cs),mi=false,j=0,w=0;
for(var i=0;
i<s.length;
++i){var x=intAt(s,i);
if(x<0){if(s.charAt(i)=="-"&&this.signum()==0){mi=true
}continue
}w=b*w+x;
if(++j>=cs){this.dMultiply(d);
this.dAddOffset(w,0);
j=0;
w=0
}}if(j>0){this.dMultiply(Math.pow(b,j));
this.dAddOffset(w,0)
}if(mi){BigInteger.ZERO.subTo(this,this)
}}function bnpFromNumber(a,b,c){if("number"==typeof b){if(a<2){this.fromInt(1)
}else{this.fromNumber(a,c);
if(!this.testBit(a-1)){this.bitwiseTo(BigInteger.ONE.shiftLeft(a-1),op_or,this)
}if(this.isEven()){this.dAddOffset(1,0)
}while(!this.isProbablePrime(b)){this.dAddOffset(2,0);
if(this.bitLength()>a){this.subTo(BigInteger.ONE.shiftLeft(a-1),this)
}}}}else{var x=new Array(),t=a&7;
x.length=(a>>3)+1;
b.nextBytes(x);
if(t>0){x[0]&=((1<<t)-1)
}else{x[0]=0
}this.fromString(x,256)
}}function bnToByteArray(){var i=this.t,r=new Array();
r[0]=this.s;
var p=this.DB-(i*this.DB)%8,d,k=0;
if(i-->0){if(p<this.DB&&(d=this[i]>>p)!=(this.s&this.DM)>>p){r[k++]=d|(this.s<<(this.DB-p))
}while(i>=0){if(p<8){d=(this[i]&((1<<p)-1))<<(8-p);
d|=this[--i]>>(p+=this.DB-8)
}else{d=(this[i]>>(p-=8))&255;
if(p<=0){p+=this.DB;
--i
}}if((d&128)!=0){d|=-256
}if(k==0&&(this.s&128)!=(d&128)){++k
}if(k>0||d!=this.s){r[k++]=d
}}}return r
}function bnEquals(a){return(this.compareTo(a)==0)
}function bnMin(a){return(this.compareTo(a)<0)?this:a
}function bnMax(a){return(this.compareTo(a)>0)?this:a
}function bnpBitwiseTo(a,op,r){var i,f,m=Math.min(a.t,this.t);
for(i=0;
i<m;
++i){r[i]=op(this[i],a[i])
}if(a.t<this.t){f=a.s&this.DM;
for(i=m;
i<this.t;
++i){r[i]=op(this[i],f)
}r.t=this.t
}else{f=this.s&this.DM;
for(i=m;
i<a.t;
++i){r[i]=op(f,a[i])
}r.t=a.t
}r.s=op(this.s,a.s);
r.clamp()
}function op_and(x,y){return x&y
}function bnAnd(a){var r=nbi();
this.bitwiseTo(a,op_and,r);
return r
}function op_or(x,y){return x|y
}function bnOr(a){var r=nbi();
this.bitwiseTo(a,op_or,r);
return r
}function op_xor(x,y){return x^y
}function bnXor(a){var r=nbi();
this.bitwiseTo(a,op_xor,r);
return r
}function op_andnot(x,y){return x&~y
}function bnAndNot(a){var r=nbi();
this.bitwiseTo(a,op_andnot,r);
return r
}function bnNot(){var r=nbi();
for(var i=0;
i<this.t;
++i){r[i]=this.DM&~this[i]
}r.t=this.t;
r.s=~this.s;
return r
}function bnShiftLeft(n){var r=nbi();
if(n<0){this.rShiftTo(-n,r)
}else{this.lShiftTo(n,r)
}return r
}function bnShiftRight(n){var r=nbi();
if(n<0){this.lShiftTo(-n,r)
}else{this.rShiftTo(n,r)
}return r
}function lbit(x){if(x==0){return -1
}var r=0;
if((x&65535)==0){x>>=16;
r+=16
}if((x&255)==0){x>>=8;
r+=8
}if((x&15)==0){x>>=4;
r+=4
}if((x&3)==0){x>>=2;
r+=2
}if((x&1)==0){++r
}return r
}function bnGetLowestSetBit(){for(var i=0;
i<this.t;
++i){if(this[i]!=0){return i*this.DB+lbit(this[i])
}}if(this.s<0){return this.t*this.DB
}return -1
}function cbit(x){var r=0;
while(x!=0){x&=x-1;
++r
}return r
}function bnBitCount(){var r=0,x=this.s&this.DM;
for(var i=0;
i<this.t;
++i){r+=cbit(this[i]^x)
}return r
}function bnTestBit(n){var j=Math.floor(n/this.DB);
if(j>=this.t){return(this.s!=0)
}return((this[j]&(1<<(n%this.DB)))!=0)
}function bnpChangeBit(n,op){var r=BigInteger.ONE.shiftLeft(n);
this.bitwiseTo(r,op,r);
return r
}function bnSetBit(n){return this.changeBit(n,op_or)
}function bnClearBit(n){return this.changeBit(n,op_andnot)
}function bnFlipBit(n){return this.changeBit(n,op_xor)
}function bnpAddTo(a,r){var i=0,c=0,m=Math.min(a.t,this.t);
while(i<m){c+=this[i]+a[i];
r[i++]=c&this.DM;
c>>=this.DB
}if(a.t<this.t){c+=a.s;
while(i<this.t){c+=this[i];
r[i++]=c&this.DM;
c>>=this.DB
}c+=this.s
}else{c+=this.s;
while(i<a.t){c+=a[i];
r[i++]=c&this.DM;
c>>=this.DB
}c+=a.s
}r.s=(c<0)?-1:0;
if(c>0){r[i++]=c
}else{if(c<-1){r[i++]=this.DV+c
}}r.t=i;
r.clamp()
}function bnAdd(a){var r=nbi();
this.addTo(a,r);
return r
}function bnSubtract(a){var r=nbi();
this.subTo(a,r);
return r
}function bnMultiply(a){var r=nbi();
this.multiplyTo(a,r);
return r
}function bnSquare(){var r=nbi();
this.squareTo(r);
return r
}function bnDivide(a){var r=nbi();
this.divRemTo(a,r,null);
return r
}function bnRemainder(a){var r=nbi();
this.divRemTo(a,null,r);
return r
}function bnDivideAndRemainder(a){var q=nbi(),r=nbi();
this.divRemTo(a,q,r);
return new Array(q,r)
}function bnpDMultiply(n){this[this.t]=this.am(0,n-1,this,0,0,this.t);
++this.t;
this.clamp()
}function bnpDAddOffset(n,w){if(n==0){return
}while(this.t<=w){this[this.t++]=0
}this[w]+=n;
while(this[w]>=this.DV){this[w]-=this.DV;
if(++w>=this.t){this[this.t++]=0
}++this[w]
}}function NullExp(){}function nNop(x){return x
}function nMulTo(x,y,r){x.multiplyTo(y,r)
}function nSqrTo(x,r){x.squareTo(r)
}NullExp.prototype.convert=nNop;
NullExp.prototype.revert=nNop;
NullExp.prototype.mulTo=nMulTo;
NullExp.prototype.sqrTo=nSqrTo;
function bnPow(e){return this.exp(e,new NullExp())
}function bnpMultiplyLowerTo(a,n,r){var i=Math.min(this.t+a.t,n);
r.s=0;
r.t=i;
while(i>0){r[--i]=0
}var j;
for(j=r.t-this.t;
i<j;
++i){r[i+this.t]=this.am(0,a[i],r,i,0,this.t)
}for(j=Math.min(a.t,n);
i<j;
++i){this.am(0,a[i],r,i,0,n-i)
}r.clamp()
}function bnpMultiplyUpperTo(a,n,r){--n;
var i=r.t=this.t+a.t-n;
r.s=0;
while(--i>=0){r[i]=0
}for(i=Math.max(n-this.t,0);
i<a.t;
++i){r[this.t+i-n]=this.am(n-i,a[i],r,0,0,this.t+i-n)
}r.clamp();
r.drShiftTo(1,r)
}function Barrett(m){this.r2=nbi();
this.q3=nbi();
BigInteger.ONE.dlShiftTo(2*m.t,this.r2);
this.mu=this.r2.divide(m);
this.m=m
}function barrettConvert(x){if(x.s<0||x.t>2*this.m.t){return x.mod(this.m)
}else{if(x.compareTo(this.m)<0){return x
}else{var r=nbi();
x.copyTo(r);
this.reduce(r);
return r
}}}function barrettRevert(x){return x
}function barrettReduce(x){x.drShiftTo(this.m.t-1,this.r2);
if(x.t>this.m.t+1){x.t=this.m.t+1;
x.clamp()
}this.mu.multiplyUpperTo(this.r2,this.m.t+1,this.q3);
this.m.multiplyLowerTo(this.q3,this.m.t+1,this.r2);
while(x.compareTo(this.r2)<0){x.dAddOffset(1,this.m.t+1)
}x.subTo(this.r2,x);
while(x.compareTo(this.m)>=0){x.subTo(this.m,x)
}}function barrettSqrTo(x,r){x.squareTo(r);
this.reduce(r)
}function barrettMulTo(x,y,r){x.multiplyTo(y,r);
this.reduce(r)
}Barrett.prototype.convert=barrettConvert;
Barrett.prototype.revert=barrettRevert;
Barrett.prototype.reduce=barrettReduce;
Barrett.prototype.mulTo=barrettMulTo;
Barrett.prototype.sqrTo=barrettSqrTo;
function bnModPow(e,m){var i=e.bitLength(),k,r=nbv(1),z;
if(i<=0){return r
}else{if(i<18){k=1
}else{if(i<48){k=3
}else{if(i<144){k=4
}else{if(i<768){k=5
}else{k=6
}}}}}if(i<8){z=new Classic(m)
}else{if(m.isEven()){z=new Barrett(m)
}else{z=new Montgomery(m)
}}var g=new Array(),n=3,k1=k-1,km=(1<<k)-1;
g[1]=z.convert(this);
if(k>1){var g2=nbi();
z.sqrTo(g[1],g2);
while(n<=km){g[n]=nbi();
z.mulTo(g2,g[n-2],g[n]);
n+=2
}}var j=e.t-1,w,is1=true,r2=nbi(),t;
i=nbits(e[j])-1;
while(j>=0){if(i>=k1){w=(e[j]>>(i-k1))&km
}else{w=(e[j]&((1<<(i+1))-1))<<(k1-i);
if(j>0){w|=e[j-1]>>(this.DB+i-k1)
}}n=k;
while((w&1)==0){w>>=1;
--n
}if((i-=n)<0){i+=this.DB;
--j
}if(is1){g[w].copyTo(r);
is1=false
}else{while(n>1){z.sqrTo(r,r2);
z.sqrTo(r2,r);
n-=2
}if(n>0){z.sqrTo(r,r2)
}else{t=r;
r=r2;
r2=t
}z.mulTo(r2,g[w],r)
}while(j>=0&&(e[j]&(1<<i))==0){z.sqrTo(r,r2);
t=r;
r=r2;
r2=t;
if(--i<0){i=this.DB-1;
--j
}}}return z.revert(r)
}function bnGCD(a){var x=(this.s<0)?this.negate():this.clone();
var y=(a.s<0)?a.negate():a.clone();
if(x.compareTo(y)<0){var t=x;
x=y;
y=t
}var i=x.getLowestSetBit(),g=y.getLowestSetBit();
if(g<0){return x
}if(i<g){g=i
}if(g>0){x.rShiftTo(g,x);
y.rShiftTo(g,y)
}while(x.signum()>0){if((i=x.getLowestSetBit())>0){x.rShiftTo(i,x)
}if((i=y.getLowestSetBit())>0){y.rShiftTo(i,y)
}if(x.compareTo(y)>=0){x.subTo(y,x);
x.rShiftTo(1,x)
}else{y.subTo(x,y);
y.rShiftTo(1,y)
}}if(g>0){y.lShiftTo(g,y)
}return y
}function bnpModInt(n){if(n<=0){return 0
}var d=this.DV%n,r=(this.s<0)?n-1:0;
if(this.t>0){if(d==0){r=this[0]%n
}else{for(var i=this.t-1;
i>=0;
--i){r=(d*r+this[i])%n
}}}return r
}function bnModInverse(m){var ac=m.isEven();
if((this.isEven()&&ac)||m.signum()==0){return BigInteger.ZERO
}var u=m.clone(),v=this.clone();
var a=nbv(1),b=nbv(0),c=nbv(0),d=nbv(1);
while(u.signum()!=0){while(u.isEven()){u.rShiftTo(1,u);
if(ac){if(!a.isEven()||!b.isEven()){a.addTo(this,a);
b.subTo(m,b)
}a.rShiftTo(1,a)
}else{if(!b.isEven()){b.subTo(m,b)
}}b.rShiftTo(1,b)
}while(v.isEven()){v.rShiftTo(1,v);
if(ac){if(!c.isEven()||!d.isEven()){c.addTo(this,c);
d.subTo(m,d)
}c.rShiftTo(1,c)
}else{if(!d.isEven()){d.subTo(m,d)
}}d.rShiftTo(1,d)
}if(u.compareTo(v)>=0){u.subTo(v,u);
if(ac){a.subTo(c,a)
}b.subTo(d,b)
}else{v.subTo(u,v);
if(ac){c.subTo(a,c)
}d.subTo(b,d)
}}if(v.compareTo(BigInteger.ONE)!=0){return BigInteger.ZERO
}if(d.compareTo(m)>=0){return d.subtract(m)
}if(d.signum()<0){d.addTo(m,d)
}else{return d
}if(d.signum()<0){return d.add(m)
}else{return d
}}var lowprimes=[2,3,5,7,11,13,17,19,23,29,31,37,41,43,47,53,59,61,67,71,73,79,83,89,97,101,103,107,109,113,127,131,137,139,149,151,157,163,167,173,179,181,191,193,197,199,211,223,227,229,233,239,241,251,257,263,269,271,277,281,283,293,307,311,313,317,331,337,347,349,353,359,367,373,379,383,389,397,401,409,419,421,431,433,439,443,449,457,461,463,467,479,487,491,499,503,509,521,523,541,547,557,563,569,571,577,587,593,599,601,607,613,617,619,631,641,643,647,653,659,661,673,677,683,691,701,709,719,727,733,739,743,751,757,761,769,773,787,797,809,811,821,823,827,829,839,853,857,859,863,877,881,883,887,907,911,919,929,937,941,947,953,967,971,977,983,991,997];
var lplim=(1<<26)/lowprimes[lowprimes.length-1];
function bnIsProbablePrime(t){var i,x=this.abs();
if(x.t==1&&x[0]<=lowprimes[lowprimes.length-1]){for(i=0;
i<lowprimes.length;
++i){if(x[0]==lowprimes[i]){return true
}}return false
}if(x.isEven()){return false
}i=1;
while(i<lowprimes.length){var m=lowprimes[i],j=i+1;
while(j<lowprimes.length&&m<lplim){m*=lowprimes[j++]
}m=x.modInt(m);
while(i<j){if(m%lowprimes[i++]==0){return false
}}}return x.millerRabin(t)
}function bnpMillerRabin(t){var n1=this.subtract(BigInteger.ONE);
var k=n1.getLowestSetBit();
if(k<=0){return false
}var r=n1.shiftRight(k);
t=(t+1)>>1;
if(t>lowprimes.length){t=lowprimes.length
}var a=nbi();
for(var i=0;
i<t;
++i){a.fromInt(lowprimes[Math.floor(Math.random()*lowprimes.length)]);
var y=a.modPow(r,this);
if(y.compareTo(BigInteger.ONE)!=0&&y.compareTo(n1)!=0){var j=1;
while(j++<k&&y.compareTo(n1)!=0){y=y.modPowInt(2,this);
if(y.compareTo(BigInteger.ONE)==0){return false
}}if(y.compareTo(n1)!=0){return false
}}}return true
}BigInteger.prototype.chunkSize=bnpChunkSize;
BigInteger.prototype.toRadix=bnpToRadix;
BigInteger.prototype.fromRadix=bnpFromRadix;
BigInteger.prototype.fromNumber=bnpFromNumber;
BigInteger.prototype.bitwiseTo=bnpBitwiseTo;
BigInteger.prototype.changeBit=bnpChangeBit;
BigInteger.prototype.addTo=bnpAddTo;
BigInteger.prototype.dMultiply=bnpDMultiply;
BigInteger.prototype.dAddOffset=bnpDAddOffset;
BigInteger.prototype.multiplyLowerTo=bnpMultiplyLowerTo;
BigInteger.prototype.multiplyUpperTo=bnpMultiplyUpperTo;
BigInteger.prototype.modInt=bnpModInt;
BigInteger.prototype.millerRabin=bnpMillerRabin;
BigInteger.prototype.clone=bnClone;
BigInteger.prototype.intValue=bnIntValue;
BigInteger.prototype.byteValue=bnByteValue;
BigInteger.prototype.shortValue=bnShortValue;
BigInteger.prototype.signum=bnSigNum;
BigInteger.prototype.toByteArray=bnToByteArray;
BigInteger.prototype.equals=bnEquals;
BigInteger.prototype.min=bnMin;
BigInteger.prototype.max=bnMax;
BigInteger.prototype.and=bnAnd;
BigInteger.prototype.or=bnOr;
BigInteger.prototype.xor=bnXor;
BigInteger.prototype.andNot=bnAndNot;
BigInteger.prototype.not=bnNot;
BigInteger.prototype.shiftLeft=bnShiftLeft;
BigInteger.prototype.shiftRight=bnShiftRight;
BigInteger.prototype.getLowestSetBit=bnGetLowestSetBit;
BigInteger.prototype.bitCount=bnBitCount;
BigInteger.prototype.testBit=bnTestBit;
BigInteger.prototype.setBit=bnSetBit;
BigInteger.prototype.clearBit=bnClearBit;
BigInteger.prototype.flipBit=bnFlipBit;
BigInteger.prototype.add=bnAdd;
BigInteger.prototype.subtract=bnSubtract;
BigInteger.prototype.multiply=bnMultiply;
BigInteger.prototype.divide=bnDivide;
BigInteger.prototype.remainder=bnRemainder;
BigInteger.prototype.divideAndRemainder=bnDivideAndRemainder;
BigInteger.prototype.modPow=bnModPow;
BigInteger.prototype.modInverse=bnModInverse;
BigInteger.prototype.pow=bnPow;
BigInteger.prototype.gcd=bnGCD;
BigInteger.prototype.isProbablePrime=bnIsProbablePrime;
BigInteger.prototype.square=bnSquare;
function ECFieldElementFp(q,x){this.x=x;
this.q=q
}function feFpEquals(other){if(other==this){return true
}return(this.q.equals(other.q)&&this.x.equals(other.x))
}function feFpToBigInteger(){return this.x
}function feFpNegate(){return new ECFieldElementFp(this.q,this.x.negate().mod(this.q))
}function feFpAdd(b){return new ECFieldElementFp(this.q,this.x.add(b.toBigInteger()).mod(this.q))
}function feFpSubtract(b){return new ECFieldElementFp(this.q,this.x.subtract(b.toBigInteger()).mod(this.q))
}function feFpMultiply(b){return new ECFieldElementFp(this.q,this.x.multiply(b.toBigInteger()).mod(this.q))
}function feFpSquare(){return new ECFieldElementFp(this.q,this.x.square().mod(this.q))
}function feFpDivide(b){return new ECFieldElementFp(this.q,this.x.multiply(b.toBigInteger().modInverse(this.q)).mod(this.q))
}ECFieldElementFp.prototype.equals=feFpEquals;
ECFieldElementFp.prototype.toBigInteger=feFpToBigInteger;
ECFieldElementFp.prototype.negate=feFpNegate;
ECFieldElementFp.prototype.add=feFpAdd;
ECFieldElementFp.prototype.subtract=feFpSubtract;
ECFieldElementFp.prototype.multiply=feFpMultiply;
ECFieldElementFp.prototype.square=feFpSquare;
ECFieldElementFp.prototype.divide=feFpDivide;
function ECPointFp(curve,x,y,z){this.curve=curve;
this.x=x;
this.y=y;
if(z==null){this.z=BigInteger.ONE
}else{this.z=z
}this.zinv=null
}function pointFpGetX(){if(this.zinv==null){this.zinv=this.z.modInverse(this.curve.q)
}return this.curve.fromBigInteger(this.x.toBigInteger().multiply(this.zinv).mod(this.curve.q))
}function pointFpGetY(){if(this.zinv==null){this.zinv=this.z.modInverse(this.curve.q)
}return this.curve.fromBigInteger(this.y.toBigInteger().multiply(this.zinv).mod(this.curve.q))
}function pointFpEquals(other){if(other==this){return true
}if(this.isInfinity()){return other.isInfinity()
}if(other.isInfinity()){return this.isInfinity()
}var u,v;
u=other.y.toBigInteger().multiply(this.z).subtract(this.y.toBigInteger().multiply(other.z)).mod(this.curve.q);
if(!u.equals(BigInteger.ZERO)){return false
}v=other.x.toBigInteger().multiply(this.z).subtract(this.x.toBigInteger().multiply(other.z)).mod(this.curve.q);
return v.equals(BigInteger.ZERO)
}function pointFpIsInfinity(){if((this.x==null)&&(this.y==null)){return true
}return this.z.equals(BigInteger.ZERO)&&!this.y.toBigInteger().equals(BigInteger.ZERO)
}function pointFpNegate(){return new ECPointFp(this.curve,this.x,this.y.negate(),this.z)
}function pointFpAdd(b){if(this.isInfinity()){return b
}if(b.isInfinity()){return this
}var u=b.y.toBigInteger().multiply(this.z).subtract(this.y.toBigInteger().multiply(b.z)).mod(this.curve.q);
var v=b.x.toBigInteger().multiply(this.z).subtract(this.x.toBigInteger().multiply(b.z)).mod(this.curve.q);
if(BigInteger.ZERO.equals(v)){if(BigInteger.ZERO.equals(u)){return this.twice()
}return this.curve.getInfinity()
}var THREE=new BigInteger("3");
var x1=this.x.toBigInteger();
var y1=this.y.toBigInteger();
var x2=b.x.toBigInteger();
var y2=b.y.toBigInteger();
var v2=v.square();
var v3=v2.multiply(v);
var x1v2=x1.multiply(v2);
var zu2=u.square().multiply(this.z);
var x3=zu2.subtract(x1v2.shiftLeft(1)).multiply(b.z).subtract(v3).multiply(v).mod(this.curve.q);
var y3=x1v2.multiply(THREE).multiply(u).subtract(y1.multiply(v3)).subtract(zu2.multiply(u)).multiply(b.z).add(u.multiply(v3)).mod(this.curve.q);
var z3=v3.multiply(this.z).multiply(b.z).mod(this.curve.q);
return new ECPointFp(this.curve,this.curve.fromBigInteger(x3),this.curve.fromBigInteger(y3),z3)
}function pointFpTwice(){if(this.isInfinity()){return this
}if(this.y.toBigInteger().signum()==0){return this.curve.getInfinity()
}var THREE=new BigInteger("3");
var x1=this.x.toBigInteger();
var y1=this.y.toBigInteger();
var y1z1=y1.multiply(this.z);
var y1sqz1=y1z1.multiply(y1).mod(this.curve.q);
var a=this.curve.a.toBigInteger();
var w=x1.square().multiply(THREE);
if(!BigInteger.ZERO.equals(a)){w=w.add(this.z.square().multiply(a))
}w=w.mod(this.curve.q);
var x3=w.square().subtract(x1.shiftLeft(3).multiply(y1sqz1)).shiftLeft(1).multiply(y1z1).mod(this.curve.q);
var y3=w.multiply(THREE).multiply(x1).subtract(y1sqz1.shiftLeft(1)).shiftLeft(2).multiply(y1sqz1).subtract(w.square().multiply(w)).mod(this.curve.q);
var z3=y1z1.square().multiply(y1z1).shiftLeft(3).mod(this.curve.q);
return new ECPointFp(this.curve,this.curve.fromBigInteger(x3),this.curve.fromBigInteger(y3),z3)
}function pointFpMultiply(k){if(this.isInfinity()){return this
}if(k.signum()==0){return this.curve.getInfinity()
}var e=k;
var h=e.multiply(new BigInteger("3"));
var neg=this.negate();
var R=this;
var i;
for(i=h.bitLength()-2;
i>0;
--i){R=R.twice();
var hBit=h.testBit(i);
var eBit=e.testBit(i);
if(hBit!=eBit){R=R.add(hBit?this:neg)
}}return R
}function pointFpMultiplyTwo(j,x,k){var i;
if(j.bitLength()>k.bitLength()){i=j.bitLength()-1
}else{i=k.bitLength()-1
}var R=this.curve.getInfinity();
var both=this.add(x);
while(i>=0){R=R.twice();
if(j.testBit(i)){if(k.testBit(i)){R=R.add(both)
}else{R=R.add(this)
}}else{if(k.testBit(i)){R=R.add(x)
}}--i
}return R
}ECPointFp.prototype.getX=pointFpGetX;
ECPointFp.prototype.getY=pointFpGetY;
ECPointFp.prototype.equals=pointFpEquals;
ECPointFp.prototype.isInfinity=pointFpIsInfinity;
ECPointFp.prototype.negate=pointFpNegate;
ECPointFp.prototype.add=pointFpAdd;
ECPointFp.prototype.twice=pointFpTwice;
ECPointFp.prototype.multiply=pointFpMultiply;
ECPointFp.prototype.multiplyTwo=pointFpMultiplyTwo;
function ECCurveFp(q,a,b){this.q=q;
this.a=this.fromBigInteger(a);
this.b=this.fromBigInteger(b);
this.infinity=new ECPointFp(this,null,null)
}function curveFpGetQ(){return this.q
}function curveFpGetA(){return this.a
}function curveFpGetB(){return this.b
}function curveFpEquals(other){if(other==this){return true
}return(this.q.equals(other.q)&&this.a.equals(other.a)&&this.b.equals(other.b))
}function curveFpGetInfinity(){return this.infinity
}function curveFpFromBigInteger(x){return new ECFieldElementFp(this.q,x)
}function curveFpDecodePointHex(s){switch(parseInt(s.substr(0,2),16)){case 0:return this.infinity;
case 2:case 3:return null;
case 4:case 6:case 7:var len=(s.length-2)/2;
var xHex=s.substr(2,len);
var yHex=s.substr(len+2,len);
return new ECPointFp(this,this.fromBigInteger(new BigInteger(xHex,16)),this.fromBigInteger(new BigInteger(yHex,16)));
default:return null
}}ECCurveFp.prototype.getQ=curveFpGetQ;
ECCurveFp.prototype.getA=curveFpGetA;
ECCurveFp.prototype.getB=curveFpGetB;
ECCurveFp.prototype.equals=curveFpEquals;
ECCurveFp.prototype.getInfinity=curveFpGetInfinity;
ECCurveFp.prototype.fromBigInteger=curveFpFromBigInteger;
ECCurveFp.prototype.decodePointHex=curveFpDecodePointHex;
function X9ECParameters(curve,g,n,h){this.curve=curve;
this.g=g;
this.n=n;
this.h=h
}function x9getCurve(){return this.curve
}function x9getG(){return this.g
}function x9getN(){return this.n
}function x9getH(){return this.h
}X9ECParameters.prototype.getCurve=x9getCurve;
X9ECParameters.prototype.getG=x9getG;
X9ECParameters.prototype.getN=x9getN;
X9ECParameters.prototype.getH=x9getH;
function fromHex(s){return new BigInteger(s,16)
}function secp128r1(){var p=fromHex("FFFFFFFDFFFFFFFFFFFFFFFFFFFFFFFF");
var a=fromHex("FFFFFFFDFFFFFFFFFFFFFFFFFFFFFFFC");
var b=fromHex("E87579C11079F43DD824993C2CEE5ED3");
var n=fromHex("FFFFFFFE0000000075A30D1B9038A115");
var h=BigInteger.ONE;
var curve=new ECCurveFp(p,a,b);
var G=curve.decodePointHex("04161FF7528B899B2D0C28607CA52C5B86CF5AC8395BAFEB13C02DA292DDED7A83");
return new X9ECParameters(curve,G,n,h)
}function secp160k1(){var p=fromHex("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFAC73");
var a=BigInteger.ZERO;
var b=fromHex("7");
var n=fromHex("0100000000000000000001B8FA16DFAB9ACA16B6B3");
var h=BigInteger.ONE;
var curve=new ECCurveFp(p,a,b);
var G=curve.decodePointHex("043B4C382CE37AA192A4019E763036F4F5DD4D7EBB938CF935318FDCED6BC28286531733C3F03C4FEE");
return new X9ECParameters(curve,G,n,h)
}function secp160r1(){var p=fromHex("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF7FFFFFFF");
var a=fromHex("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF7FFFFFFC");
var b=fromHex("1C97BEFC54BD7A8B65ACF89F81D4D4ADC565FA45");
var n=fromHex("0100000000000000000001F4C8F927AED3CA752257");
var h=BigInteger.ONE;
var curve=new ECCurveFp(p,a,b);
var G=curve.decodePointHex("044A96B5688EF573284664698968C38BB913CBFC8223A628553168947D59DCC912042351377AC5FB32");
return new X9ECParameters(curve,G,n,h)
}function secp192k1(){var p=fromHex("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFEE37");
var a=BigInteger.ZERO;
var b=fromHex("3");
var n=fromHex("FFFFFFFFFFFFFFFFFFFFFFFE26F2FC170F69466A74DEFD8D");
var h=BigInteger.ONE;
var curve=new ECCurveFp(p,a,b);
var G=curve.decodePointHex("04DB4FF10EC057E9AE26B07D0280B7F4341DA5D1B1EAE06C7D9B2F2F6D9C5628A7844163D015BE86344082AA88D95E2F9D");
return new X9ECParameters(curve,G,n,h)
}function secp192r1(){var p=fromHex("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFF");
var a=fromHex("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFC");
var b=fromHex("64210519E59C80E70FA7E9AB72243049FEB8DEECC146B9B1");
var n=fromHex("FFFFFFFFFFFFFFFFFFFFFFFF99DEF836146BC9B1B4D22831");
var h=BigInteger.ONE;
var curve=new ECCurveFp(p,a,b);
var G=curve.decodePointHex("04188DA80EB03090F67CBF20EB43A18800F4FF0AFD82FF101207192B95FFC8DA78631011ED6B24CDD573F977A11E794811");
return new X9ECParameters(curve,G,n,h)
}function secp224r1(){var p=fromHex("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000001");
var a=fromHex("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFE");
var b=fromHex("B4050A850C04B3ABF54132565044B0B7D7BFD8BA270B39432355FFB4");
var n=fromHex("FFFFFFFFFFFFFFFFFFFFFFFFFFFF16A2E0B8F03E13DD29455C5C2A3D");
var h=BigInteger.ONE;
var curve=new ECCurveFp(p,a,b);
var G=curve.decodePointHex("04B70E0CBD6BB4BF7F321390B94A03C1D356C21122343280D6115C1D21BD376388B5F723FB4C22DFE6CD4375A05A07476444D5819985007E34");
return new X9ECParameters(curve,G,n,h)
}function secp256k1(){var p=fromHex("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F");
var a=BigInteger.ZERO;
var b=fromHex("7");
var n=fromHex("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141");
var h=BigInteger.ONE;
var curve=new ECCurveFp(p,a,b);
var G=curve.decodePointHex("0479BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8");
return new X9ECParameters(curve,G,n,h)
}function secp256r1(){var p=fromHex("FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF");
var a=fromHex("FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFC");
var b=fromHex("5AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B");
var n=fromHex("FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551");
var h=BigInteger.ONE;
var curve=new ECCurveFp(p,a,b);
var G=curve.decodePointHex("046B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C2964FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5");
return new X9ECParameters(curve,G,n,h)
}function getSECCurveByName(name){if(name=="secp128r1"){return secp128r1()
}if(name=="secp160k1"){return secp160k1()
}if(name=="secp160r1"){return secp160r1()
}if(name=="secp192k1"){return secp192k1()
}if(name=="secp192r1"){return secp192r1()
}if(name=="secp224r1"){return secp224r1()
}if(name=="secp256k1"){return secp256k1()
}if(name=="secp256r1"){return secp256r1()
}return null
}var EventEmitter=function(){};
EventEmitter.prototype.on=function(name,callback,context){if(!context){context=this
}if(!this._listeners){this._listeners={}
}if(!this._listeners[name]){this._listeners[name]=[]
}if(!this._unbinders){this._unbinders={}
}if(!this._unbinders[name]){this._unbinders[name]=[]
}var f=function(e){callback.apply(context,[e])
};
this._unbinders[name].push(callback);
this._listeners[name].push(f)
};
EventEmitter.prototype.trigger=function(name,event){if(event===undefined){event={}
}if(!this._listeners){this._listeners={}
}if(!this._listeners[name]){return
}var i=this._listeners[name].length;
while(i--){this._listeners[name][i](event)
}};
EventEmitter.prototype.removeListener=function(name,callback){if(!this._unbinders){this._unbinders={}
}if(!this._unbinders[name]){return
}var i=this._unbinders[name].length;
while(i--){if(this._unbinders[name][i]===callback){this._unbinders[name].splice(i,1);
this._listeners[name].splice(i,1)
}}};
EventEmitter.augment=function(obj){for(var method in EventEmitter.prototype){if(!obj[method]){obj[method]=EventEmitter.prototype[method]
}}};
(function(exports){var Bitcoin=exports;
if("object"!==typeof module){Bitcoin.EventEmitter=EventEmitter
}})("object"===typeof module?module.exports:(window.Bitcoin={}));
BigInteger.valueOf=nbv;
BigInteger.prototype.toByteArrayUnsigned=function(){var ba=this.abs().toByteArray();
if(ba.length){if(ba[0]==0){ba=ba.slice(1)
}return ba.map(function(v){return(v<0)?v+256:v
})
}else{return ba
}};
BigInteger.fromByteArrayUnsigned=function(ba){if(!ba.length){return ba.valueOf(0)
}else{if(ba[0]&128){return new BigInteger([0].concat(ba))
}else{return new BigInteger(ba)
}}};
BigInteger.prototype.toByteArraySigned=function(){var val=this.abs().toByteArrayUnsigned();
var neg=this.compareTo(BigInteger.ZERO)<0;
if(neg){if(val[0]&128){val.unshift(128)
}else{val[0]|=128
}}else{if(val[0]&128){val.unshift(0)
}}return val
};
BigInteger.fromByteArraySigned=function(ba){if(ba[0]&128){ba[0]&=127;
return BigInteger.fromByteArrayUnsigned(ba).negate()
}else{return BigInteger.fromByteArrayUnsigned(ba)
}};
var names=["log","debug","info","warn","error","assert","dir","dirxml","group","groupEnd","time","timeEnd","count","trace","profile","profileEnd"];
if("undefined"==typeof window.console){window.console={}
}for(var i=0;
i<names.length;
++i){if("undefined"==typeof window.console[names[i]]){window.console[names[i]]=function(){}
}}Bitcoin.Util={isArray:Array.isArray||function(o){return Object.prototype.toString.call(o)==="[object Array]"
},makeFilledArray:function(len,val){var array=[];
var i=0;
while(i<len){array[i++]=val
}return array
},numToVarInt:function(i){if(i<253){return[i]
}else{if(i<=1<<16){return[253,i>>>8,i&255]
}else{if(i<=1<<32){return[254].concat(Crypto.util.wordsToBytes([i]))
}else{return[255].concat(Crypto.util.wordsToBytes([i>>>32,i]))
}}}},valueToBigInt:function(valueBuffer){if(valueBuffer instanceof BigInteger){return valueBuffer
}return BigInteger.fromByteArrayUnsigned(valueBuffer)
},formatValue:function(valueBuffer){var value=this.valueToBigInt(valueBuffer).toString();
var integerPart=value.length>8?value.substr(0,value.length-8):"0";
var decimalPart=value.length>8?value.substr(value.length-8):value;
while(decimalPart.length<8){decimalPart="0"+decimalPart
}decimalPart=decimalPart.replace(/0*$/,"");
while(decimalPart.length<2){decimalPart+="0"
}return integerPart+"."+decimalPart
},parseValue:function(valueString){var valueComp=valueString.split(".");
var integralPart=valueComp[0];
var fractionalPart=valueComp[1]||"0";
while(fractionalPart.length<8){fractionalPart+="0"
}fractionalPart=fractionalPart.replace(/^0+/g,"");
var value=BigInteger.valueOf(parseInt(integralPart));
value=value.multiply(BigInteger.valueOf(100000000));
value=value.add(BigInteger.valueOf(parseInt(fractionalPart)));
return value
},sha256ripe160:function(data){return Crypto.RIPEMD160(Crypto.SHA256(data,{asBytes:true}),{asBytes:true})
}};
for(var i in Crypto.util){if(Crypto.util.hasOwnProperty(i)){Bitcoin.Util[i]=Crypto.util[i]
}}(function(Bitcoin){Bitcoin.Base58={alphabet:"123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz",validRegex:/^[1-9A-HJ-NP-Za-km-z]+$/,base:BigInteger.valueOf(58),encode:function(input){var bi=BigInteger.fromByteArrayUnsigned(input);
var chars=[];
while(bi.compareTo(B58.base)>=0){var mod=bi.mod(B58.base);
chars.unshift(B58.alphabet[mod.intValue()]);
bi=bi.subtract(mod).divide(B58.base)
}chars.unshift(B58.alphabet[bi.intValue()]);
for(var i=0;
i<input.length;
i++){if(input[i]==0){chars.unshift(B58.alphabet[0])
}else{break
}}return chars.join("")
},decode:function(input){var bi=BigInteger.valueOf(0);
var leadingZerosNum=0;
for(var i=input.length-1;
i>=0;
i--){var alphaIndex=B58.alphabet.indexOf(input[i]);
if(alphaIndex<0){throw"Invalid character"
}bi=bi.add(BigInteger.valueOf(alphaIndex).multiply(B58.base.pow(input.length-1-i)));
if(input[i]=="1"){leadingZerosNum++
}else{leadingZerosNum=0
}}var bytes=bi.toByteArrayUnsigned();
while(leadingZerosNum-->0){bytes.unshift(0)
}return bytes
}};
var B58=Bitcoin.Base58
})("undefined"!=typeof Bitcoin?Bitcoin:module.exports);
Bitcoin.Address=function(bytes){if("string"==typeof bytes){bytes=Bitcoin.Address.decodeString(bytes)
}this.hash=bytes;
this.version=0
};
Bitcoin.Address.prototype.toString=function(){var hash=this.hash.slice(0);
hash.unshift(this.version);
var checksum=Crypto.SHA256(Crypto.SHA256(hash,{asBytes:true}),{asBytes:true});
var bytes=hash.concat(checksum.slice(0,4));
return Bitcoin.Base58.encode(bytes)
};
Bitcoin.Address.prototype.getHashBase64=function(){return Crypto.util.bytesToBase64(this.hash)
};
Bitcoin.Address.decodeString=function(string){var bytes=Bitcoin.Base58.decode(string);
var hash=bytes.slice(0,21);
var checksum=Crypto.SHA256(Crypto.SHA256(hash,{asBytes:true}),{asBytes:true});
if(checksum[0]!=bytes[21]||checksum[1]!=bytes[22]||checksum[2]!=bytes[23]||checksum[3]!=bytes[24]){throw"Checksum validation failed!"
}var version=hash.shift();
if(version!=0){throw"Version "+version+" not supported!"
}return hash
};
function integerToBytes(i,len){var bytes=i.toByteArrayUnsigned();
if(len<bytes.length){bytes=bytes.slice(bytes.length-len)
}else{while(len>bytes.length){bytes.unshift(0)
}}return bytes
}ECFieldElementFp.prototype.getByteLength=function(){return Math.floor((this.toBigInteger().bitLength()+7)/8)
};
ECPointFp.prototype.getEncoded=function(compressed){var x=this.getX().toBigInteger();
var y=this.getY().toBigInteger();
var enc=integerToBytes(x,32);
if(compressed){if(y.isEven()){enc.unshift(2)
}else{enc.unshift(3)
}}else{enc.unshift(4);
enc=enc.concat(integerToBytes(y,32))
}return enc
};
ECPointFp.decodeFrom=function(curve,enc){var type=enc[0];
var dataLen=enc.length-1;
var xBa=enc.slice(1,1+dataLen/2);
var yBa=enc.slice(1+dataLen/2,1+dataLen);
xBa.unshift(0);
yBa.unshift(0);
var x=new BigInteger(xBa);
var y=new BigInteger(yBa);
return new ECPointFp(curve,curve.fromBigInteger(x),curve.fromBigInteger(y))
};
ECPointFp.prototype.add2D=function(b){if(this.isInfinity()){return b
}if(b.isInfinity()){return this
}if(this.x.equals(b.x)){if(this.y.equals(b.y)){return this.twice()
}return this.curve.getInfinity()
}var x_x=b.x.subtract(this.x);
var y_y=b.y.subtract(this.y);
var gamma=y_y.divide(x_x);
var x3=gamma.square().subtract(this.x).subtract(b.x);
var y3=gamma.multiply(this.x.subtract(x3)).subtract(this.y);
return new ECPointFp(this.curve,x3,y3)
};
ECPointFp.prototype.twice2D=function(){if(this.isInfinity()){return this
}if(this.y.toBigInteger().signum()==0){return this.curve.getInfinity()
}var TWO=this.curve.fromBigInteger(BigInteger.valueOf(2));
var THREE=this.curve.fromBigInteger(BigInteger.valueOf(3));
var gamma=this.x.square().multiply(THREE).add(this.curve.a).divide(this.y.multiply(TWO));
var x3=gamma.square().subtract(this.x.multiply(TWO));
var y3=gamma.multiply(this.x.subtract(x3)).subtract(this.y);
return new ECPointFp(this.curve,x3,y3)
};
ECPointFp.prototype.multiply2D=function(k){if(this.isInfinity()){return this
}if(k.signum()==0){return this.curve.getInfinity()
}var e=k;
var h=e.multiply(new BigInteger("3"));
var neg=this.negate();
var R=this;
var i;
for(i=h.bitLength()-2;
i>0;
--i){R=R.twice();
var hBit=h.testBit(i);
var eBit=e.testBit(i);
if(hBit!=eBit){R=R.add2D(hBit?this:neg)
}}return R
};
ECPointFp.prototype.isOnCurve=function(){var x=this.getX().toBigInteger();
var y=this.getY().toBigInteger();
var a=this.curve.getA().toBigInteger();
var b=this.curve.getB().toBigInteger();
var n=this.curve.getQ();
var lhs=y.multiply(y).mod(n);
var rhs=x.multiply(x).multiply(x).add(a.multiply(x)).add(b).mod(n);
return lhs.equals(rhs)
};
ECPointFp.prototype.toString=function(){return"("+this.getX().toBigInteger().toString()+","+this.getY().toBigInteger().toString()+")"
};
ECPointFp.prototype.validate=function(){var n=this.curve.getQ();
if(this.isInfinity()){throw new Error("Point is at infinity.")
}var x=this.getX().toBigInteger();
var y=this.getY().toBigInteger();
if(x.compareTo(BigInteger.ONE)<0||x.compareTo(n.subtract(BigInteger.ONE))>0){throw new Error("x coordinate out of bounds")
}if(y.compareTo(BigInteger.ONE)<0||y.compareTo(n.subtract(BigInteger.ONE))>0){throw new Error("y coordinate out of bounds")
}if(!this.isOnCurve()){throw new Error("Point is not on the curve.")
}if(this.multiply(n).isInfinity()){throw new Error("Point is not a scalar multiple of G.")
}return true
};
function dmp(v){if(!(v instanceof BigInteger)){v=v.toBigInteger()
}return Crypto.util.bytesToHex(v.toByteArrayUnsigned())
}Bitcoin.ECDSA=(function(){var ecparams=getSECCurveByName("secp256k1");
var rng=new SecureRandom();
var P_OVER_FOUR=null;
function implShamirsTrick(P,k,Q,l){var m=Math.max(k.bitLength(),l.bitLength());
var Z=P.add2D(Q);
var R=P.curve.getInfinity();
for(var i=m-1;
i>=0;
--i){R=R.twice2D();
R.z=BigInteger.ONE;
if(k.testBit(i)){if(l.testBit(i)){R=R.add2D(Z)
}else{R=R.add2D(P)
}}else{if(l.testBit(i)){R=R.add2D(Q)
}}}return R
}var ECDSA={getBigRandom:function(limit){return new BigInteger(limit.bitLength(),rng).mod(limit.subtract(BigInteger.ONE)).add(BigInteger.ONE)
},sign:function(hash,priv){var d=priv;
var n=ecparams.getN();
var e=BigInteger.fromByteArrayUnsigned(hash);
do{var k=ECDSA.getBigRandom(n);
var G=ecparams.getG();
var Q=G.multiply(k);
var r=Q.getX().toBigInteger().mod(n)
}while(r.compareTo(BigInteger.ZERO)<=0);
var s=k.modInverse(n).multiply(e.add(d.multiply(r))).mod(n);
return ECDSA.serializeSig(r,s)
},verify:function(hash,sig,pubkey){var r,s;
if(Bitcoin.Util.isArray(sig)){var obj=ECDSA.parseSig(sig);
r=obj.r;
s=obj.s
}else{if("object"===typeof sig&&sig.r&&sig.s){r=sig.r;
s=sig.s
}else{throw"Invalid value for signature"
}}var Q;
if(pubkey instanceof ECPointFp){Q=pubkey
}else{if(Bitcoin.Util.isArray(pubkey)){Q=ECPointFp.decodeFrom(ecparams.getCurve(),pubkey)
}else{throw"Invalid format for pubkey value, must be byte array or ECPointFp"
}}var e=BigInteger.fromByteArrayUnsigned(hash);
return ECDSA.verifyRaw(e,r,s,Q)
},verifyRaw:function(e,r,s,Q){var n=ecparams.getN();
var G=ecparams.getG();
if(r.compareTo(BigInteger.ONE)<0||r.compareTo(n)>=0){return false
}if(s.compareTo(BigInteger.ONE)<0||s.compareTo(n)>=0){return false
}var c=s.modInverse(n);
var u1=e.multiply(c).mod(n);
var u2=r.multiply(c).mod(n);
var point=G.multiply(u1).add(Q.multiply(u2));
var v=point.getX().toBigInteger().mod(n);
return v.equals(r)
},serializeSig:function(r,s){var rBa=r.toByteArraySigned();
var sBa=s.toByteArraySigned();
var sequence=[];
sequence.push(2);
sequence.push(rBa.length);
sequence=sequence.concat(rBa);
sequence.push(2);
sequence.push(sBa.length);
sequence=sequence.concat(sBa);
sequence.unshift(sequence.length);
sequence.unshift(48);
return sequence
},parseSig:function(sig){var cursor;
if(sig[0]!=48){throw new Error("Signature not a valid DERSequence")
}cursor=2;
if(sig[cursor]!=2){throw new Error("First element in signature must be a DERInteger")
}var rBa=sig.slice(cursor+2,cursor+2+sig[cursor+1]);
cursor+=2+sig[cursor+1];
if(sig[cursor]!=2){throw new Error("Second element in signature must be a DERInteger")
}var sBa=sig.slice(cursor+2,cursor+2+sig[cursor+1]);
cursor+=2+sig[cursor+1];
var r=BigInteger.fromByteArrayUnsigned(rBa);
var s=BigInteger.fromByteArrayUnsigned(sBa);
return{r:r,s:s}
},parseSigCompact:function(sig){if(sig.length!==65){throw"Signature has the wrong length"
}var i=sig[0]-27;
if(i<0||i>7){throw"Invalid signature type"
}var n=ecparams.getN();
var r=BigInteger.fromByteArrayUnsigned(sig.slice(1,33)).mod(n);
var s=BigInteger.fromByteArrayUnsigned(sig.slice(33,65)).mod(n);
return{r:r,s:s,i:i}
},recoverPubKey:function(r,s,hash,i){i=i&3;
var isYEven=i&1;
var isSecondKey=i>>1;
var n=ecparams.getN();
var G=ecparams.getG();
var curve=ecparams.getCurve();
var p=curve.getQ();
var a=curve.getA().toBigInteger();
var b=curve.getB().toBigInteger();
if(!P_OVER_FOUR){P_OVER_FOUR=p.add(BigInteger.ONE).divide(BigInteger.valueOf(4))
}var x=isSecondKey?r.add(n):r;
var alpha=x.multiply(x).multiply(x).add(a.multiply(x)).add(b).mod(p);
var beta=alpha.modPow(P_OVER_FOUR,p);
var xorOdd=beta.isEven()?(i%2):((i+1)%2);
var y=(beta.isEven()?!isYEven:isYEven)?beta:p.subtract(beta);
var R=new ECPointFp(curve,curve.fromBigInteger(x),curve.fromBigInteger(y));
R.validate();
var e=BigInteger.fromByteArrayUnsigned(hash);
var eNeg=BigInteger.ZERO.subtract(e).mod(n);
var rInv=r.modInverse(n);
var Q=implShamirsTrick(R,s,G,eNeg).multiply(rInv);
Q.validate();
if(!ECDSA.verifyRaw(e,r,s,Q)){throw"Pubkey recovery unsuccessful"
}var pubKey=new Bitcoin.ECKey();
pubKey.pub=Q;
return pubKey
},calcPubkeyRecoveryParam:function(address,r,s,hash){for(var i=0;
i<4;
i++){try{var pubkey=Bitcoin.ECDSA.recoverPubKey(r,s,hash,i);
if(pubkey.getBitcoinAddress().toString()==address){return i
}}catch(e){}}throw"Unable to find valid recovery factor"
}};
return ECDSA
})();
Bitcoin.ECKey=(function(){var ECDSA=Bitcoin.ECDSA;
var ecparams=getSECCurveByName("secp256k1");
var rng=new SecureRandom();
var ECKey=function(input){if(!input){var n=ecparams.getN();
this.priv=ECDSA.getBigRandom(n)
}else{if(input instanceof BigInteger){this.priv=input
}else{if(Bitcoin.Util.isArray(input)){this.priv=BigInteger.fromByteArrayUnsigned(input)
}else{if("string"==typeof input){if(input.length==51&&input[0]=="5"){this.priv=BigInteger.fromByteArrayUnsigned(ECKey.decodeString(input))
}else{this.priv=BigInteger.fromByteArrayUnsigned(Crypto.util.base64ToBytes(input))
}}}}}this.compressed=!!ECKey.compressByDefault
};
ECKey.compressByDefault=false;
ECKey.prototype.setCompressed=function(v){this.compressed=!!v
};
ECKey.prototype.getPub=function(){return this.getPubPoint().getEncoded(this.compressed)
};
ECKey.prototype.getPubPoint=function(){if(!this.pub){this.pub=ecparams.getG().multiply(this.priv)
}return this.pub
};
ECKey.prototype.getPubKeyHash=function(){if(this.pubKeyHash){return this.pubKeyHash
}return this.pubKeyHash=Bitcoin.Util.sha256ripe160(this.getPub())
};
ECKey.prototype.getBitcoinAddress=function(){var hash=this.getPubKeyHash();
var addr=new Bitcoin.Address(hash);
return addr
};
ECKey.prototype.getExportedPrivateKey=function(){var hash=this.priv.toByteArrayUnsigned();
while(hash.length<32){hash.unshift(0)
}hash.unshift(128);
var checksum=Crypto.SHA256(Crypto.SHA256(hash,{asBytes:true}),{asBytes:true});
var bytes=hash.concat(checksum.slice(0,4));
return Bitcoin.Base58.encode(bytes)
};
ECKey.prototype.setPub=function(pub){this.pub=ECPointFp.decodeFrom(ecparams.getCurve(),pub)
};
ECKey.prototype.toString=function(format){if(format==="base64"){return Crypto.util.bytesToBase64(this.priv.toByteArrayUnsigned())
}else{return Crypto.util.bytesToHex(this.priv.toByteArrayUnsigned())
}};
ECKey.prototype.sign=function(hash){return ECDSA.sign(hash,this.priv)
};
ECKey.prototype.verify=function(hash,sig){return ECDSA.verify(hash,sig,this.getPub())
};
ECKey.decodeString=function(string){var bytes=Bitcoin.Base58.decode(string);
var hash=bytes.slice(0,33);
var checksum=Crypto.SHA256(Crypto.SHA256(hash,{asBytes:true}),{asBytes:true});
if(checksum[0]!=bytes[33]||checksum[1]!=bytes[34]||checksum[2]!=bytes[35]||checksum[3]!=bytes[36]){throw"Checksum validation failed!"
}var version=hash.shift();
if(version!=128){throw"Version "+version+" not supported!"
}return hash
};
return ECKey
})();
(function(){var Opcode=Bitcoin.Opcode=function(num){this.code=num
};
Opcode.prototype.toString=function(){return Opcode.reverseMap[this.code]
};
Opcode.map={OP_0:0,OP_FALSE:0,OP_PUSHDATA1:76,OP_PUSHDATA2:77,OP_PUSHDATA4:78,OP_1NEGATE:79,OP_RESERVED:80,OP_1:81,OP_TRUE:81,OP_2:82,OP_3:83,OP_4:84,OP_5:85,OP_6:86,OP_7:87,OP_8:88,OP_9:89,OP_10:90,OP_11:91,OP_12:92,OP_13:93,OP_14:94,OP_15:95,OP_16:96,OP_NOP:97,OP_VER:98,OP_IF:99,OP_NOTIF:100,OP_VERIF:101,OP_VERNOTIF:102,OP_ELSE:103,OP_ENDIF:104,OP_VERIFY:105,OP_RETURN:106,OP_TOALTSTACK:107,OP_FROMALTSTACK:108,OP_2DROP:109,OP_2DUP:110,OP_3DUP:111,OP_2OVER:112,OP_2ROT:113,OP_2SWAP:114,OP_IFDUP:115,OP_DEPTH:116,OP_DROP:117,OP_DUP:118,OP_NIP:119,OP_OVER:120,OP_PICK:121,OP_ROLL:122,OP_ROT:123,OP_SWAP:124,OP_TUCK:125,OP_CAT:126,OP_SUBSTR:127,OP_LEFT:128,OP_RIGHT:129,OP_SIZE:130,OP_INVERT:131,OP_AND:132,OP_OR:133,OP_XOR:134,OP_EQUAL:135,OP_EQUALVERIFY:136,OP_RESERVED1:137,OP_RESERVED2:138,OP_1ADD:139,OP_1SUB:140,OP_2MUL:141,OP_2DIV:142,OP_NEGATE:143,OP_ABS:144,OP_NOT:145,OP_0NOTEQUAL:146,OP_ADD:147,OP_SUB:148,OP_MUL:149,OP_DIV:150,OP_MOD:151,OP_LSHIFT:152,OP_RSHIFT:153,OP_BOOLAND:154,OP_BOOLOR:155,OP_NUMEQUAL:156,OP_NUMEQUALVERIFY:157,OP_NUMNOTEQUAL:158,OP_LESSTHAN:159,OP_GREATERTHAN:160,OP_LESSTHANOREQUAL:161,OP_GREATERTHANOREQUAL:162,OP_MIN:163,OP_MAX:164,OP_WITHIN:165,OP_RIPEMD160:166,OP_SHA1:167,OP_SHA256:168,OP_HASH160:169,OP_HASH256:170,OP_CODESEPARATOR:171,OP_CHECKSIG:172,OP_CHECKSIGVERIFY:173,OP_CHECKMULTISIG:174,OP_CHECKMULTISIGVERIFY:175,OP_NOP1:176,OP_NOP2:177,OP_NOP3:178,OP_NOP4:179,OP_NOP5:180,OP_NOP6:181,OP_NOP7:182,OP_NOP8:183,OP_NOP9:184,OP_NOP10:185,OP_PUBKEYHASH:253,OP_PUBKEY:254,OP_INVALIDOPCODE:255};
Opcode.reverseMap=[];
for(var i in Opcode.map){Opcode.reverseMap[Opcode.map[i]]=i
}})();
(function(){var Opcode=Bitcoin.Opcode;
for(var i in Opcode.map){eval("var "+i+" = "+Opcode.map[i]+";")
}var Script=Bitcoin.Script=function(data){if(!data){this.buffer=[]
}else{if("string"==typeof data){this.buffer=Crypto.util.base64ToBytes(data)
}else{if(Bitcoin.Util.isArray(data)){this.buffer=data
}else{if(data instanceof Script){this.buffer=data.buffer
}else{throw new Error("Invalid script")
}}}}this.parse()
};
Script.prototype.parse=function(){var self=this;
this.chunks=[];
var i=0;
function readChunk(n){self.chunks.push(self.buffer.slice(i,i+n));
i+=n
}while(i<this.buffer.length){var opcode=this.buffer[i++];
if(opcode>=240){opcode=(opcode<<8)|this.buffer[i++]
}var len;
if(opcode>0&&opcode<OP_PUSHDATA1){readChunk(opcode)
}else{if(opcode==OP_PUSHDATA1){len=this.buffer[i++];
readChunk(len)
}else{if(opcode==OP_PUSHDATA2){len=(this.buffer[i++]<<8)|this.buffer[i++];
readChunk(len)
}else{if(opcode==OP_PUSHDATA4){len=(this.buffer[i++]<<24)|(this.buffer[i++]<<16)|(this.buffer[i++]<<8)|this.buffer[i++];
readChunk(len)
}else{this.chunks.push(opcode)
}}}}}};
Script.prototype.getOutType=function(){if(this.chunks[this.chunks.length-1]==OP_CHECKMULTISIG&&this.chunks[this.chunks.length-2]<=3){return"Multisig"
}else{if(this.chunks.length==5&&this.chunks[0]==OP_DUP&&this.chunks[1]==OP_HASH160&&this.chunks[3]==OP_EQUALVERIFY&&this.chunks[4]==OP_CHECKSIG){return"Address"
}else{if(this.chunks.length==2&&this.chunks[1]==OP_CHECKSIG){return"Pubkey"
}else{return"Strange"
}}}};
Script.prototype.simpleOutHash=function(){switch(this.getOutType()){case"Address":return this.chunks[2];
case"Pubkey":return Bitcoin.Util.sha256ripe160(this.chunks[0]);
default:throw new Error("Encountered non-standard scriptPubKey")
}};
Script.prototype.simpleOutPubKeyHash=Script.prototype.simpleOutHash;
Script.prototype.getInType=function(){if(this.chunks.length==1&&Bitcoin.Util.isArray(this.chunks[0])){return"Pubkey"
}else{if(this.chunks.length==2&&Bitcoin.Util.isArray(this.chunks[0])&&Bitcoin.Util.isArray(this.chunks[1])){return"Address"
}else{return"Strange"
}}};
Script.prototype.simpleInPubKey=function(){switch(this.getInType()){case"Address":return this.chunks[1];
case"Pubkey":throw new Error("Script does not contain pubkey.");
default:throw new Error("Encountered non-standard scriptSig")
}};
Script.prototype.simpleInHash=function(){return Bitcoin.Util.sha256ripe160(this.simpleInPubKey())
};
Script.prototype.simpleInPubKeyHash=Script.prototype.simpleInHash;
Script.prototype.writeOp=function(opcode){this.buffer.push(opcode);
this.chunks.push(opcode)
};
Script.prototype.writeBytes=function(data){if(data.length<OP_PUSHDATA1){this.buffer.push(data.length)
}else{if(data.length<=255){this.buffer.push(OP_PUSHDATA1);
this.buffer.push(data.length)
}else{if(data.length<=65535){this.buffer.push(OP_PUSHDATA2);
this.buffer.push(data.length&255);
this.buffer.push((data.length>>>8)&255)
}else{this.buffer.push(OP_PUSHDATA4);
this.buffer.push(data.length&255);
this.buffer.push((data.length>>>8)&255);
this.buffer.push((data.length>>>16)&255);
this.buffer.push((data.length>>>24)&255)
}}}this.buffer=this.buffer.concat(data);
this.chunks.push(data)
};
Script.createOutputScript=function(address){var script=new Script();
script.writeOp(OP_DUP);
script.writeOp(OP_HASH160);
script.writeBytes(address.hash);
script.writeOp(OP_EQUALVERIFY);
script.writeOp(OP_CHECKSIG);
return script
};
Script.prototype.extractAddresses=function(addresses){switch(this.getOutType()){case"Address":addresses.push(new Address(this.chunks[2]));
return 1;
case"Pubkey":addresses.push(new Address(Util.sha256ripe160(this.chunks[0])));
return 1;
case"Multisig":for(var i=1;
i<this.chunks.length-2;
++i){addresses.push(new Address(Util.sha256ripe160(this.chunks[i])))
}return this.chunks[0]-OP_1+1;
default:throw new Error("Encountered non-standard scriptPubKey")
}};
Script.createMultiSigOutputScript=function(m,pubkeys){var script=new Bitcoin.Script();
script.writeOp(OP_1+m-1);
for(var i=0;
i<pubkeys.length;
++i){script.writeBytes(pubkeys[i])
}script.writeOp(OP_1+pubkeys.length-1);
script.writeOp(OP_CHECKMULTISIG);
return script
};
Script.createInputScript=function(signature,pubKey){var script=new Script();
script.writeBytes(signature);
script.writeBytes(pubKey);
return script
};
Script.prototype.clone=function(){return new Script(this.buffer)
}
})();
(function(){var Script=Bitcoin.Script;
var Transaction=Bitcoin.Transaction=function(doc){this.version=1;
this.lock_time=0;
this.ins=[];
this.outs=[];
this.timestamp=null;
this.block=null;
if(doc){if(doc.hash){this.hash=doc.hash
}if(doc.version){this.version=doc.version
}if(doc.lock_time){this.lock_time=doc.lock_time
}if(doc.ins&&doc.ins.length){for(var i=0;
i<doc.ins.length;
i++){this.addInput(new TransactionIn(doc.ins[i]))
}}if(doc.outs&&doc.outs.length){for(var i=0;
i<doc.outs.length;
i++){this.addOutput(new TransactionOut(doc.outs[i]))
}}if(doc.timestamp){this.timestamp=doc.timestamp
}if(doc.block){this.block=doc.block
}}};
Transaction.objectify=function(txs){var objs=[];
for(var i=0;
i<txs.length;
i++){objs.push(new Transaction(txs[i]))
}return objs
};
Transaction.prototype.addInput=function(tx,outIndex){if(arguments[0] instanceof TransactionIn){this.ins.push(arguments[0])
}else{this.ins.push(new TransactionIn({outpoint:{hash:tx.hash,index:outIndex},script:new Bitcoin.Script(),sequence:4294967295}))
}};
Transaction.prototype.addOutput=function(address,value){if(arguments[0] instanceof TransactionOut){this.outs.push(arguments[0])
}else{if(value instanceof BigInteger){value=value.toByteArrayUnsigned().reverse();
while(value.length<8){value.push(0)
}}else{if(Bitcoin.Util.isArray(value)){}}this.outs.push(new TransactionOut({value:value,script:Script.createOutputScript(address)}))
}};
Transaction.prototype.serialize=function(){var buffer=[];
buffer=buffer.concat(Crypto.util.wordsToBytes([parseInt(this.version)]).reverse());
buffer=buffer.concat(Bitcoin.Util.numToVarInt(this.ins.length));
for(var i=0;
i<this.ins.length;
i++){var txin=this.ins[i];
buffer=buffer.concat(Crypto.util.base64ToBytes(txin.outpoint.hash));
buffer=buffer.concat(Crypto.util.wordsToBytes([parseInt(txin.outpoint.index)]).reverse());
var scriptBytes=txin.script.buffer;
buffer=buffer.concat(Bitcoin.Util.numToVarInt(scriptBytes.length));
buffer=buffer.concat(scriptBytes);
buffer=buffer.concat(Crypto.util.wordsToBytes([parseInt(txin.sequence)]).reverse())
}buffer=buffer.concat(Bitcoin.Util.numToVarInt(this.outs.length));
for(var i=0;
i<this.outs.length;
i++){var txout=this.outs[i];
buffer=buffer.concat(txout.value);
var scriptBytes=txout.script.buffer;
buffer=buffer.concat(Bitcoin.Util.numToVarInt(scriptBytes.length));
buffer=buffer.concat(scriptBytes)
}buffer=buffer.concat(Crypto.util.wordsToBytes([parseInt(this.lock_time)]).reverse());
return buffer
};
var OP_CODESEPARATOR=171;
var SIGHASH_ALL=1;
var SIGHASH_NONE=2;
var SIGHASH_SINGLE=3;
var SIGHASH_ANYONECANPAY=80;
Transaction.prototype.hashTransactionForSignature=function(connectedScript,inIndex,hashType){var txTmp=this.clone();
for(var i=0;
i<txTmp.ins.length;
i++){txTmp.ins[i].script=new Script()
}txTmp.ins[inIndex].script=connectedScript;
if((hashType&31)==SIGHASH_NONE){txTmp.outs=[];
for(var i=0;
i<txTmp.ins.length;
i++){if(i!=inIndex){txTmp.ins[i].sequence=0
}}}else{if((hashType&31)==SIGHASH_SINGLE){}}if(hashType&SIGHASH_ANYONECANPAY){txTmp.ins=[txTmp.ins[inIndex]]
}var buffer=txTmp.serialize();
buffer=buffer.concat(Crypto.util.wordsToBytes([parseInt(hashType)]).reverse());
var hash1=Crypto.SHA256(buffer,{asBytes:true});
return Crypto.SHA256(hash1,{asBytes:true})
};
Transaction.prototype.getHash=function(){var buffer=this.serialize();
return Crypto.SHA256(Crypto.SHA256(buffer,{asBytes:true}),{asBytes:true})
};
Transaction.prototype.clone=function(){var newTx=new Transaction();
newTx.version=this.version;
newTx.lock_time=this.lock_time;
for(var i=0;
i<this.ins.length;
i++){var txin=this.ins[i].clone();
newTx.addInput(txin)
}for(var i=0;
i<this.outs.length;
i++){var txout=this.outs[i].clone();
newTx.addOutput(txout)
}return newTx
};
Transaction.prototype.analyze=function(wallet){if(!(wallet instanceof Bitcoin.Wallet)){return null
}var allFromMe=true,allToMe=true,firstRecvHash=null,firstMeRecvHash=null,firstSendHash=null;
for(var i=this.outs.length-1;
i>=0;
i--){var txout=this.outs[i];
var hash=txout.script.simpleOutPubKeyHash();
if(!wallet.hasHash(hash)){allToMe=false
}else{firstMeRecvHash=hash
}firstRecvHash=hash
}for(var i=this.ins.length-1;
i>=0;
i--){var txin=this.ins[i];
firstSendHash=txin.script.simpleInPubKeyHash();
if(!wallet.hasHash(firstSendHash)){allFromMe=false;
break
}}var impact=this.calcImpact(wallet);
var analysis={};
analysis.impact=impact;
if(impact.sign>0&&impact.value.compareTo(BigInteger.ZERO)>0){analysis.type="recv";
analysis.addr=new Bitcoin.Address(firstMeRecvHash)
}else{if(allFromMe&&allToMe){analysis.type="self"
}else{if(allFromMe){analysis.type="sent";
analysis.addr=new Bitcoin.Address(firstRecvHash)
}else{analysis.type="other"
}}}return analysis
};
Transaction.prototype.getDescription=function(wallet){var analysis=this.analyze(wallet);
if(!analysis){return""
}switch(analysis.type){case"recv":return"Received with "+analysis.addr;
break;
case"sent":return"Payment to "+analysis.addr;
break;
case"self":return"Payment to yourself";
break;
case"other":default:return""
}};
Transaction.prototype.getTotalOutValue=function(){var totalValue=BigInteger.ZERO;
for(var j=0;
j<this.outs.length;
j++){var txout=this.outs[j];
totalValue=totalValue.add(Bitcoin.Util.valueToBigInt(txout.value))
}return totalValue
};
Transaction.prototype.getTotalValue=Transaction.prototype.getTotalOutValue;
Transaction.prototype.calcImpact=function(wallet){if(!(wallet instanceof Bitcoin.Wallet)){return BigInteger.ZERO
}var valueOut=BigInteger.ZERO;
for(var j=0;
j<this.outs.length;
j++){var txout=this.outs[j];
var hash=Crypto.util.bytesToBase64(txout.script.simpleOutPubKeyHash());
if(wallet.hasHash(hash)){valueOut=valueOut.add(Bitcoin.Util.valueToBigInt(txout.value))
}}var valueIn=BigInteger.ZERO;
for(var j=0;
j<this.ins.length;
j++){var txin=this.ins[j];
var hash=Crypto.util.bytesToBase64(txin.script.simpleInPubKeyHash());
if(wallet.hasHash(hash)){var fromTx=wallet.txIndex[txin.outpoint.hash];
if(fromTx){valueIn=valueIn.add(Bitcoin.Util.valueToBigInt(fromTx.outs[txin.outpoint.index].value))
}}}if(valueOut.compareTo(valueIn)>=0){return{sign:1,value:valueOut.subtract(valueIn)}
}else{return{sign:-1,value:valueIn.subtract(valueOut)}
}};
var TransactionIn=Bitcoin.TransactionIn=function(data){this.outpoint=data.outpoint;
if(data.script instanceof Script){this.script=data.script
}else{this.script=new Script(data.script)
}this.sequence=data.sequence
};
TransactionIn.prototype.clone=function(){var newTxin=new TransactionIn({outpoint:{hash:this.outpoint.hash,index:this.outpoint.index},script:this.script.clone(),sequence:this.sequence});
return newTxin
};
var TransactionOut=Bitcoin.TransactionOut=function(data){if(data.script instanceof Script){this.script=data.script
}else{this.script=new Script(data.script)
}if(Bitcoin.Util.isArray(data.value)){this.value=data.value
}else{if("string"==typeof data.value){var valueHex=(new BigInteger(data.value,10)).toString(16);
while(valueHex.length<16){valueHex="0"+valueHex
}this.value=Crypto.util.hexToBytes(valueHex)
}}};
TransactionOut.prototype.clone=function(){var newTxout=new TransactionOut({script:this.script.clone(),value:this.value.slice(0)});
return newTxout
}
})();
Bitcoin.Wallet=(function(){var Script=Bitcoin.Script,TransactionIn=Bitcoin.TransactionIn,TransactionOut=Bitcoin.TransactionOut;
var Wallet=function(){var keys=[];
this.addressHashes=[];
this.txIndex={};
this.unspentOuts=[];
this.addressPointer=0;
this.addKey=function(key,pub){if(!(key instanceof Bitcoin.ECKey)){key=new Bitcoin.ECKey(key)
}keys.push(key);
if(pub){if("string"===typeof pub){pub=Crypto.util.base64ToBytes(pub)
}key.setPub(pub)
}this.addressHashes.push(key.getBitcoinAddress().getHashBase64())
};
this.addKeys=function(keys,pubs){if("string"===typeof keys){keys=keys.split(",")
}if("string"===typeof pubs){pubs=pubs.split(",")
}var i;
if(Array.isArray(pubs)&&keys.length==pubs.length){for(i=0;
i<keys.length;
i++){this.addKey(keys[i],pubs[i])
}}else{for(i=0;
i<keys.length;
i++){this.addKey(keys[i])
}}};
this.getKeys=function(){var serializedWallet=[];
for(var i=0;
i<keys.length;
i++){serializedWallet.push(keys[i].toString("base64"))
}return serializedWallet
};
this.getPubKeys=function(){var pubs=[];
for(var i=0;
i<keys.length;
i++){pubs.push(Crypto.util.bytesToBase64(keys[i].getPub()))
}return pubs
};
this.clear=function(){keys=[]
};
this.getLength=function(){return keys.length
};
this.getAllAddresses=function(){var addresses=[];
for(var i=0;
i<keys.length;
i++){addresses.push(keys[i].getBitcoinAddress())
}return addresses
};
this.getCurAddress=function(){if(keys[this.addressPointer]){return keys[this.addressPointer].getBitcoinAddress()
}else{return null
}};
this.getNextAddress=function(){this.addressPointer++;
if(!keys[this.addressPointer]){this.generateAddress()
}return keys[this.addressPointer].getBitcoinAddress()
};
this.signWithKey=function(pubKeyHash,hash){pubKeyHash=Crypto.util.bytesToBase64(pubKeyHash);
for(var i=0;
i<this.addressHashes.length;
i++){if(this.addressHashes[i]==pubKeyHash){return keys[i].sign(hash)
}}throw new Error("Missing key for signature")
};
this.getPubKeyFromHash=function(pubKeyHash){pubKeyHash=Crypto.util.bytesToBase64(pubKeyHash);
for(var i=0;
i<this.addressHashes.length;
i++){if(this.addressHashes[i]==pubKeyHash){return keys[i].getPub()
}}throw new Error("Hash unknown")
}
};
Wallet.prototype.generateAddress=function(){this.addKey(new Bitcoin.ECKey())
};
Wallet.prototype.process=function(tx){if(this.txIndex[tx.hash]){return
}var j;
var k;
var hash;
for(j=0;
j<tx.outs.length;
j++){var txout=new TransactionOut(tx.outs[j]);
hash=Crypto.util.bytesToBase64(txout.script.simpleOutPubKeyHash());
for(k=0;
k<this.addressHashes.length;
k++){if(this.addressHashes[k]===hash){this.unspentOuts.push({tx:tx,index:j,out:txout});
break
}}}for(j=0;
j<tx.ins.length;
j++){var txin=new TransactionIn(tx.ins[j]);
var pubkey=txin.script.simpleInPubKey();
hash=Crypto.util.bytesToBase64(Bitcoin.Util.sha256ripe160(pubkey));
for(k=0;
k<this.addressHashes.length;
k++){if(this.addressHashes[k]===hash){for(var l=0;
l<this.unspentOuts.length;
l++){if(txin.outpoint.hash==this.unspentOuts[l].tx.hash&&txin.outpoint.index==this.unspentOuts[l].index){this.unspentOuts.splice(l,1)
}}break
}}}this.txIndex[tx.hash]=tx
};
Wallet.prototype.getBalance=function(){var balance=BigInteger.valueOf(0);
for(var i=0;
i<this.unspentOuts.length;
i++){var txout=this.unspentOuts[i].out;
balance=balance.add(Bitcoin.Util.valueToBigInt(txout.value))
}return balance
};
Wallet.prototype.createSend=function(address,sendValue,feeValue){var selectedOuts=[];
var txValue=sendValue.add(feeValue);
var availableValue=BigInteger.ZERO;
var i;
for(i=0;
i<this.unspentOuts.length;
i++){selectedOuts.push(this.unspentOuts[i]);
availableValue=availableValue.add(Bitcoin.Util.valueToBigInt(this.unspentOuts[i].out.value));
if(availableValue.compareTo(txValue)>=0){break
}}if(availableValue.compareTo(txValue)<0){throw new Error("Insufficient funds.")
}var changeValue=availableValue.subtract(txValue);
var sendTx=new Bitcoin.Transaction();
for(i=0;
i<selectedOuts.length;
i++){sendTx.addInput(selectedOuts[i].tx,selectedOuts[i].index)
}sendTx.addOutput(address,sendValue);
if(changeValue.compareTo(BigInteger.ZERO)>0){sendTx.addOutput(this.getNextAddress(),changeValue)
}var hashType=1;
for(i=0;
i<sendTx.ins.length;
i++){var hash=sendTx.hashTransactionForSignature(selectedOuts[i].out.script,i,hashType);
var pubKeyHash=selectedOuts[i].out.script.simpleOutPubKeyHash();
var signature=this.signWithKey(pubKeyHash,hash);
signature.push(parseInt(hashType,10));
sendTx.ins[i].script=Script.createInputScript(signature,this.getPubKeyFromHash(pubKeyHash))
}return sendTx
};
Wallet.prototype.clearTransactions=function(){this.txIndex={};
this.unspentOuts=[]
};
Wallet.prototype.hasHash=function(hash){if(Bitcoin.Util.isArray(hash)){hash=Crypto.util.bytesToBase64(hash)
}for(var k=0;
k<this.addressHashes.length;
k++){if(this.addressHashes[k]===hash){return true
}}return false
};
return Wallet
})();
var TransactionDatabase=function(){this.txs=[];
this.txIndex={}
};
EventEmitter.augment(TransactionDatabase.prototype);
TransactionDatabase.prototype.addTransaction=function(tx){this.addTransactionNoUpdate(tx);
$(this).trigger("update")
};
TransactionDatabase.prototype.addTransactionNoUpdate=function(tx){if(this.txIndex[tx.hash]){return
}this.txs.push(new Bitcoin.Transaction(tx));
this.txIndex[tx.hash]=tx
};
TransactionDatabase.prototype.removeTransaction=function(hash){this.removeTransactionNoUpdate(hash);
$(this).trigger("update")
};
TransactionDatabase.prototype.removeTransactionNoUpdate=function(hash){var tx=this.txIndex[hash];
if(!tx){return
}for(var i=0,l=this.txs.length;
i<l;
i++){if(this.txs[i].hash==hash){this.txs.splice(i,1);
break
}}delete this.txIndex[hash]
};
TransactionDatabase.prototype.loadTransactions=function(txs){for(var i=0;
i<txs.length;
i++){this.addTransactionNoUpdate(txs[i])
}$(this).trigger("update")
};
TransactionDatabase.prototype.getTransactions=function(){return this.txs
};
TransactionDatabase.prototype.clear=function(){this.txs=[];
this.txIndex={};
$(this).trigger("update")
};
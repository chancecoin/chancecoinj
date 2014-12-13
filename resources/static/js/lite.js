//TODO: make balances update when send txs happen

//CONFIG
var FEE_ADDRESS = "1CHACHAGuuxTr8Yo9b9SQmUGLg9X5iSeKX";
var PREFIX = 'CHANCECO';
var ID_ROLL = 14;
var ID_DICE = 40;
var ID_POKER = 41;
var LENGTH_ROLL = 40;
var LENGTH_ROLL2 = 48;
var LENGTH_DICE = 24;
var LENGTH_POKER = 30;
var DUST_SIZE = 780;
var MIN_FEE = 1000;
var FEE_ADDRESS_FEE = 3560;
var UNIT = 100000000;
var MAX_PROFIT = 0.01;
var HOUSE_EDGE = 0.01;
var CACHE_getTx = {};
var CACHE_getChancecoinTx = {};
var CACHE_decodeChancecoinTx = {};
var CACHE_getBTCPrice = null;
var CACHE_getCHAPrice = null;
var CACHE_getBTCBlockHeight = {};
var CACHE_getBTCBlockHash = null;
var CACHE_getBlock = {};
var CACHE_getTransactions = {};
var HOME = "https://chancecoin.github.io";
//var CORS_PROXY = "https://jsonp.nodejitsu.com/?&url=http://";
var CORS_PROXY = "http://www.corsproxy.com/";
var UPDATING = false;
var BALANCES = null;
var LOG = [];
var CONTENT = null;
var ADDRESS = null;
var PRIVATE_KEY = null;

var Base64={_keyStr:"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",encode:function(e){var t="";var n,r,i,s,o,u,a;var f=0;e=Base64._utf8_encode(e);while(f<e.length){n=e.charCodeAt(f++);r=e.charCodeAt(f++);i=e.charCodeAt(f++);s=n>>2;o=(n&3)<<4|r>>4;u=(r&15)<<2|i>>6;a=i&63;if(isNaN(r)){u=a=64}else if(isNaN(i)){a=64}t=t+this._keyStr.charAt(s)+this._keyStr.charAt(o)+this._keyStr.charAt(u)+this._keyStr.charAt(a)}return t},decode:function(e){var t="";var n,r,i;var s,o,u,a;var f=0;e=e.replace(/[^A-Za-z0-9\+\/\=]/g,"");while(f<e.length){s=this._keyStr.indexOf(e.charAt(f++));o=this._keyStr.indexOf(e.charAt(f++));u=this._keyStr.indexOf(e.charAt(f++));a=this._keyStr.indexOf(e.charAt(f++));n=s<<2|o>>4;r=(o&15)<<4|u>>2;i=(u&3)<<6|a;t=t+String.fromCharCode(n);if(u!=64){t=t+String.fromCharCode(r)}if(a!=64){t=t+String.fromCharCode(i)}}t=Base64._utf8_decode(t);return t},_utf8_encode:function(e){e=e.replace(/\r\n/g,"\n");var t="";for(var n=0;n<e.length;n++){var r=e.charCodeAt(n);if(r<128){t+=String.fromCharCode(r)}else if(r>127&&r<2048){t+=String.fromCharCode(r>>6|192);t+=String.fromCharCode(r&63|128)}else{t+=String.fromCharCode(r>>12|224);t+=String.fromCharCode(r>>6&63|128);t+=String.fromCharCode(r&63|128)}}return t},_utf8_decode:function(e){var t="";var n=0;var r=c1=c2=0;while(n<e.length){r=e.charCodeAt(n);if(r<128){t+=String.fromCharCode(r);n++}else if(r>191&&r<224){c2=e.charCodeAt(n+1);t+=String.fromCharCode((r&31)<<6|c2&63);n+=2}else{c2=e.charCodeAt(n+1);c3=e.charCodeAt(n+2);t+=String.fromCharCode((r&15)<<12|(c2&63)<<6|c3&63);n+=3}}return t}}

$(function(){
  $("[data-hide]").on("click", function(){
    $($(this).attr("data-hide")).hide();
  });
});

$(function(){
  $("[data-template]").on("click", function(){
    var page = $(this).attr("data-template");
    if (page == "home") {
      if (CONTENT) {
        $("#content").html(CONTENT);
      }
      CONTENT = null;
    } else {
      var url = CORS_PROXY + "chancecoin.github.io/templates/" + page;
      var result = download(url);
      if (!CONTENT) {
        CONTENT = $("#content").html();
      }
      $("#content").html(result);
      MathJax.Hub.Queue(["Typeset",MathJax.Hub]);
    }
  });
});

$(document).ready(function() {
  if(location.hash) {
    $('a[href=' + location.hash + ']').tab('show');
  }
  $(document.body).on("click", "a[data-toggle]", function(event) {
    location.hash = this.getAttribute("href");
  });
  initialize();
  update();
  setInterval(function(){update();}, 5000);
  setInterval(function(){clearCaches();}, 60000);
  setInterval(function(){updateBalances();}, 600000);
});

function clearCaches() {
  CACHE_getTransactions = {};
}

function update() {
  if (!UPDATING) {
    UPDATING = true;
    getCasinoInfo();
    getNewTransactions();
    UPDATING = false;
  }
}

function initialize() {
  updateAddressFromUrl();
  if (!ADDRESS) {
    ADDRESS = readCookie("address");
  }
  if (!ADDRESS) {
    $("#importPrivateKey").modal('show');
  }
  getNewPokerCards();
}

function getNewPokerCards() {
  var cards = [];
  while (cards.length<7) {
    var rand = Math.floor(Math.random()*52);
    var card = getCard(rand);
    if ($.inArray(card, cards)<0) {
      cards.push(card);
    }
  }
  cards.splice(5,0,"??","??");
  var chance = chanceOfWinning(cards)*100;
  var html = getPokerCardsHtml(cards,chance);
  $("#poker_cards").html(html);
}

function generatePrivateKey() {
  var key = new Bitcoin.ECKey.makeRandom();
  var address = key.pub.getAddress().toString();
  var privateKey = key.toWIF();
  showMessage("<p>Your new address and private key are:</p><p>Address: "+address+"</p><p>Private key: "+privateKey+"</p><p>Please write down the private key and store it somewhere safe.</p>");
  importPrivateKey(privateKey);
}

function importPrivateKey(privateKeyWIF) {
  $("ul#addresses").addClass("loading");
  var privateKey;
  if (privateKeyWIF) {
    privateKey = privateKeyWIF;
  } else {
    privateKey = $( "input[name=privatekey]" ).val();
  }
  try {
    var key = new Bitcoin.ECKey.fromWIF(privateKey);
    var address = key.pub.getAddress().toString();
    var addresses = JSON.parse(readCookie("addresses"));
    var privateKeys = JSON.parse(readCookie("private_keys"));
    if (addresses == null) {
      addresses = [];
    }
    if (addresses.indexOf(address) < 0) {
      addresses.push(address);
    }
    if (privateKeys == null) {
      privateKeys = [];
    }
    if (privateKeys.indexOf(privateKey) < 0) {
      privateKeys.push(privateKey);
    }
    ADDRESS = address;
    PRIVATE_KEY = privateKey;
    eraseCookie("address");
    eraseCookie("addresses");
    eraseCookie("private_key");
    eraseCookie("private_keys");
    createCookie("address", address, 999999);
    createCookie("addresses", JSON.stringify(addresses), 999999);
    createCookie("private_key", privateKey, 999999);
    createCookie("private_keys", JSON.stringify(privateKeys), 999999);
  } catch (e) {
  }
  update();
  $("input[name=privatekey]").val("");
  $("#importPrivateKey").modal('hide');
}

//update the page using the address passed in
function updateAddress(newAddress) {
  var addresses = JSON.parse(readCookie("addresses"));
  var privateKeys = JSON.parse(readCookie("private_keys"));
  var newPrivateKey = "";
  if (addresses && privateKeys) {
    var addressIndex = addresses.indexOf(newAddress);
    newPrivateKey = addressIndex >= 0 ? privateKeys[addressIndex] : "";
  }
  ADDRESS = newAddress;
  PRIVATE_KEY = newPrivateKey;
}

//update the page using the address from URL
function updateAddressFromUrl() {
  var newAddress = getParameterByName("address");
  if (newAddress != null) {
    updateAddress(newAddress);
  }
}
function getParameterByName(name) {
  name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
  var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
    results = regex.exec(location.search);
  return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}
function updateAddressDropDown(addressInfos) {
  var addressSelected = ADDRESS;
  if (addressSelected) {
    $("#address").html(addressSelected.substring(0,6)+'... <b class="caret"></b>');
  }

  var listItems = $("#addresses").children();
  if (listItems) {
    for (var i in listItems) {
      if (i > 0) {
        listItems[i].remove();
      }
    }
  }

  if (addressInfos) {
    for (var i in addressInfos) {
      var addressInfo = addressInfos[i];
      var address = addressInfo["address"];
      var balanceCHA = addressInfo["balanceCHA"];
      if (address == addressSelected) {
        $("#addresses").append('<li id="address_'+i+'"><a href="?address='+address+'"><strong>'+ address+'</strong> <span class="badge"><span id="cha_balance_'+i+'">' + balanceCHA.toFixed(5) + '</span> CHA</span></a></li>');
      } else {
        $("#addresses").append('<li id="address_'+i+'"><a href="?address='+address+'">'+ address+' <span class="badge"><span id="cha_balance_'+i+'">' + balanceCHA.toFixed(5) + '</span> CHA</span></a></li>');
      }
    }
  }
}
function pushTx(txHex) {
  var url = "http://api.bitwatch.co/pushtx/"+txHex;
  var result = false;
  var data = download(url);
  if (data && (data.status == 200 || data.status == 201)) {
    result = true;
  }
  return result;
}
function getUnspents(address) {
  var url = "http://api.bitwatch.co/listunspent/"+address+"?verbose=1&minconf=0";
  var unspents = [];
  var data = download(url);
  if (data) {
    $.each(data.result, function(i,result){
      unspents.push(result);
    });
  }
  return unspents;
}
function getTransactions(address) {
  if (CACHE_getTransactions[address]) {
    return CACHE_getTransactions[address];
  } else {
    var url = "https://insight.bitpay.com/api/addrs/"+address+"/txs?from=0&to=75";
    var txs = [];
    var data = download(url);
    if (data) {
      var txs = [];
      $.each(data.items, function(i,result){
        txs.push(result);
      });
    }
    CACHE_getTransactions[address] = txs;
    return txs;
  }
}
function toFixed(value, precision) {
  var precision = precision || 0,
    power = Math.pow(10, precision),
    absValue = Math.abs(Math.round(value * power)),
    result = (value < 0 ? '-' : '') + String(Math.floor(absValue / power));
  if (precision > 0) {
    var fraction = String(absValue % power),
      padding = new Array(Math.max(precision - fraction.length, 0) + 1).join('0');
    result += '.' + padding + fraction;
  }
  return result;
}

function createDiceBet(bet, resolution, asset, address, chance, payout) {
  var chaSupply = CHASupplyForBetting();
  var balance = getBalance(address, "CHA");
  if (source=="") {
    showError("Please specify a source address.");
    return;
  }
  if (!(bet>0)) {
    showError("Please bet more than zero.");
    return;
  }
  if (!(chance>0.0 && chance<100.0)) {
    showError("Please specify a chance between 0 and 100.");
    return;
  }
  if (!(payout>1.0)) {
    showError("Please specify a payout greater than 1.");
    return;
  }
  if (!(toFixed(chance,6)==toFixed(100.0/(payout/(1.0-HOUSE_EDGE)),6))) {
    showError("Please specify a chance and payout that are congruent.");
    return;
  }
  if (!(bet<=balance)) {
    showError("Please specify a bet that is smaller than your CHA balance."); return;
  }
  if (!((payout-1.0)*bet<chaSupply*MAX_PROFIT)) {
    showError("Please specify a bet with a payout less than the maximum percentage of the house bankroll you can win.");
    return;
  }
  btcAmount = 0;

  if (resolution == "instant") {
    destination = FEE_ADDRESS;
    btcAmount = FEE_ADDRESS_FEE;
  }
  if (asset=="BTC") {
    destination = FEE_ADDRESS;
    btcAmount = FEE_ADDRESS_FEE + bet;
    bet = 0;
  }

  jsp = new JSPack();
  var byteBuffer = jsp.Pack(">8sIQdd", [PREFIX, ID_DICE, bet*UNIT, chance, payout]);

  var source = address;
  var destinations = [destination];
  var btcAmounts = [btcAmount];
  var fee = MIN_FEE;
  var data = byteBuffer;
  var useUnspentTxHash = "";
  var useUnspentVout = -1;
  var tx = createTransaction(source, destinations, btcAmounts, fee, data, useUnspentTxHash, useUnspentVout);
  var result = false;
  if (tx) {
    result = pushTx(tx.toHex());
  }
  return result;
}

function createPokerBet(bet, resolution, asset, address, cards) {
  var chaSupply = CHASupplyForBetting();
  var balance = getBalance(address, "CHA");

  if ($.unique(cards.slice()).length!=8) {
    showError("Please select unique cards.");
    return;
  }

  var card_numbers = cards.map(function(card) {return cardToNumber(card);});
  var chance = chanceOfWinning(cards)*100;
  var payout = 100/chance*(1-HOUSE_EDGE);

  if (source=="") {
    showError("Please specify a source address.");
    return;
  }
  if (!(bet>0)) {
    showError("Please bet more than zero.");
    return;
  }
  if (!(chance>0.0 && chance<100.0)) {
    showError("Please specify a chance between 0 and 100.");
    return;
  }
  if (!(payout>1.0)) {
    showError("Please specify a payout greater than 1.");
    return;
  }
  if (!(toFixed(chance,6)==toFixed(100.0/(payout/(1.0-HOUSE_EDGE)),6))) {
    showError("Please specify a chance and payout that are congruent.");
    return;
  }
  if (!(bet<=balance)) {
    showError("Please specify a bet that is smaller than your CHA balance."); return;
  }
  if (!((payout-1.0)*bet<chaSupply*MAX_PROFIT)) {
    showError("Please specify a bet with a payout less than the maximum percentage of the house bankroll you can win.");
    return;
  }
  btcAmount = 0;

  if (resolution == "instant") {
    destination = FEE_ADDRESS;
    btcAmount = FEE_ADDRESS_FEE;
  }
  if (asset=="BTC") {
    destination = FEE_ADDRESS;
    btcAmount = FEE_ADDRESS_FEE + bet;
    bet = 0;
  }

  jsp = new JSPack();
  var byteBuffer = jsp.Pack(">8sIQ9h4x", [PREFIX, ID_POKER, bet*UNIT].concat(card_numbers).concat([0,0,0,0]));

  var source = address;
  var destinations = [destination];
  var btcAmounts = [btcAmount];
  var fee = MIN_FEE;
  var data = byteBuffer;
  var useUnspentTxHash = "";
  var useUnspentVout = -1;
  var tx = createTransaction(source, destinations, btcAmounts, fee, data, useUnspentTxHash, useUnspentVout);
  var result = false;
  if (tx) {
    result = pushTx(tx.toHex());
  }
  return result;
}

function processBet(formName) {
    var address = ADDRESS;
    var bet = $( "#"+formName+" input[name=bet]" ).val();
    var resolution = $( "#"+formName+" input[name=resolution]" ).val();
    var asset = $( "#"+formName+" select[name=asset]" ).val();
    var chance = $( "#"+formName+" input[name=chance]" ).val();
    var payout = $( "#"+formName+" input[name=payout]" ).val();
    var card1 = $( "#"+formName+" input[name=card1]" ).val();
    var card2 = $( "#"+formName+" input[name=card2]" ).val();
    var card3 = $( "#"+formName+" input[name=card3]" ).val();
    var card4 = $( "#"+formName+" input[name=card4]" ).val();
    var card5 = $( "#"+formName+" input[name=card5]" ).val();
    var card6 = $( "#"+formName+" input[name=card6]" ).val();
    var card7 = $( "#"+formName+" input[name=card7]" ).val();
    var card8 = $( "#"+formName+" input[name=card8]" ).val();
    var card9 = $( "#"+formName+" input[name=card9]" ).val();
    if (formName=="dice" && bet && resolution && asset && address && chance && payout) {
      disableForm(formName);
      hideMessage();
      var result = createDiceBet(bet, resolution, asset, address, chance, payout);
      if (result) {
        showMessage("Thank you for betting!");
      }
      enableForm(formName);
      clearCaches();
      update();
      setTimeout(function(){clearCaches(); update();}, 2000);
    }
    if (formName=="poker" && bet && resolution && asset && address && card1 && card2 && card3 && card4 && card5 && card6 && card7 && card8 && card9) {
      disableForm(formName);
      hideMessage();
      var result = createPokerBet(bet, resolution, asset, address, [card1, card2, card3, card4, card5, card6, card7, card8, card9]);
      if (result) {
        showMessage("Thank you for betting!");
      }
      enableForm(formName);
      clearCaches();
      update();
      setTimeout(function(){clearCaches(); update();}, 2000);
    }
}
function createTransaction(source, destinations, btcAmounts, fee, data, useUnspentTxHash, useUnspentVout) {
  tx = new Bitcoin.Transaction();
  var address = ADDRESS;
  var private_key = PRIVATE_KEY;
  var ecKey = null;
  try {
    ecKey = new Bitcoin.ECKey.fromWIF(private_key);
  } catch (e) {
    showError("You do not have a valid private key for this address.");
    return;
  }
  if (destinations.length>0 && btcAmounts.length==destinations.length) {
    var destination = destinations[0];
    var btcAmount = btcAmounts[0];
    if (destination=="" || btcAmount>=DUST_SIZE) {
      var totalOutput = fee;
      var totalInput = 0;
      for (i in destinations) {
        destination = destinations[i];
        btcAmount = btcAmounts[i];
        if (destination!="" && btcAmount>0) {
          totalOutput += btcAmount;
          tx.addOutput(destination, btcAmount);
        }
      }
      for (i = 0; i<data.length; i+=32) {
        var chunk = data.slice(i, Math.min(i+32, data.length));
        chunk.splice(0,0,chunk.length);
        while (chunk.length<=32) {
          chunk.push(0);
        }
        var keys = [];
        if (source == address) {
          keys.push(ecKey);
        }
        var pubKeys = keys.map(function(x) { return x.pub });
        var pubKeyBuffers = keys.map(function(x) { return x.pub.toBuffer() });
        pubKeyBuffers.push(chunk);
        var redeemScript = new Bitcoin.scripts.multisigOutputFromPubKeyBuffers(1, pubKeyBuffers);
        var scriptPubKey = new Bitcoin.scripts.scriptHashOutput(redeemScript.getHash());
        var multisigAddress = new Bitcoin.Address.fromOutputScript(scriptPubKey).toString();
        tx.addOutput(redeemScript, DUST_SIZE);
				totalOutput = totalOutput + DUST_SIZE;
      }

      var unspents = getUnspents(source);
      var inputKeys = [];
      var inputScripts = [];
      var inputTypes = [];
      var atLeastOneRegularInput = false;
      for (i in unspents) {
        var unspent = unspents[i];
        if ((useUnspentTxHash==unspent.txid && useUnspentVout==unspent.vout) || (useUnspentVout<0 && ((unspent.type=="pubkeyhash" && (totalOutput>totalInput || !atLeastOneRegularInput)) || (unspent.type=="multisig")))) {
          var unspentTx = getTx(unspent.txid, true);
          if (!unspentTx.vout[unspent.vout].spentTxId) {
            if (unspent.type=="pubkeyhash") {
              atLeastOneRegularInput = true;
            }
            if (source == address) {
              totalInput = totalInput + unspent.amount*UNIT;
              tx.addInput(unspent.txid, unspent.vout);
              inputScripts.push(Bitcoin.Script.fromHex(unspent.scriptPubKey.hex));
              inputKeys.push(ecKey);
              inputTypes.push(unspent.type);
            }
          }
        }
      }

      if (!atLeastOneRegularInput) {
				showError("You do not have enough standard unspent outputs to cover the transaction.");
        return;
			}

			if (totalInput<totalOutput) {
				showError("You do not have enough BTC to cover the transaction.");
        return;
			}
			var totalChange = Math.floor(totalInput - totalOutput);

			if (totalChange>0) {
				tx.addOutput(source, totalChange);
			}

      key = ecKey;
      var pubKeys = inputKeys.map(function(eck) { return eck.pub })
      for (i in tx.ins) {
        var signature = tx.signInput(i, inputScripts[i], inputKeys[i]); //problem is here?
        if (inputTypes[i] == "multisig") {
          var redeemScriptSig = new Bitcoin.scripts.multisigInput([signature]);
          //var scriptSig = new Bitcoin.scripts.scriptHashInput(redeemScriptSig, inputScripts[i]);
          tx.setInputScript(i, redeemScriptSig);
        } else if (inputTypes[i] == "pubkeyhash") {
          var redeemScriptSig = new Bitcoin.scripts.pubKeyHashInput(signature, inputKeys[i].pub);
          tx.setInputScript(i, redeemScriptSig);
        }
      }
      return tx;
    }
  }
}
function getTx(txid, nocache) {
    if (nocache) {
      cache = false;
    } else {
      cache = true;
    }
    if (cache && CACHE_getTx[txid]) {
      return CACHE_getTx[txid];
    } else {
      var url = "https://insight.bitpay.com/api/tx/"+txid;
      var tx = {};
      var data = download(url, cache);
      if (data) {
        tx = data;
      }
      CACHE_getTx[txid] = tx;
      return tx;
    }
}
function createHexString(arr) {
    var result = "";
    var z;
    var len = 2;
    for (var i = 0; i < arr.length; i++) {
        var str = arr[i].toString(16);
        z = len - str.length + 1;
        str = Array(z).join("0") + str;
        result += str;
    }
    return result;
}
function parseHexString(str) {
    var result = [];
    var len = 2;
    while (str.length >= len) {
        result.push(parseInt(str.substring(0, len), 16));
        str = str.substring(len, str.length);
    }
    return result;
}
function getMessage(data) {
  return data.slice(4,data.length);
}
function getMessageType(data) {
  return data.slice(0,4);
}
function getChancecoinTx(txid) {
  if (CACHE_getChancecoinTx[txid]) {
    return CACHE_getChancecoinTx[txid];
  } else {
    tx = getTx(txid);
    var chancecoinTx = null;
    var data = [];
    for (vout in tx.vout) {
      var vout = tx.vout[vout];
      var asm = vout.scriptPubKey.asm.split(' ');
      if (asm.length==2 && asm[0]=="OP_RETURN") {
        data = data.concat(parseHexString(asm[1]));
      } else if (asm.length>=5 && asm[0]=='1' && asm[3]=='2' && asm[4]=='OP_CHECKMULTISIG') {
        var data_pubkey = parseHexString(asm[2]);
        var data_chunk_length = data_pubkey[0];
        data = data.concat(data_pubkey.slice(1,data_chunk_length+1));
      }
    }
    var prefix_bytes = PREFIX.split ('').map (function (c) { return c.charCodeAt (0); });
    if (JSON.stringify(data.slice(0,prefix_bytes.length))==JSON.stringify(prefix_bytes)) {
      data = data.slice(prefix_bytes.length,data.length);
      var message = getMessage(data);
      var messageType = getMessageType(data)[3];
      chancecoinTx = {"message": message, "messageType": messageType, "tx": tx};
    }
    CACHE_getChancecoinTx[txid] = chancecoinTx;
    if (!chancecoinTx) {
      CACHE_getTx[txid] = "ignore";
    }
    return chancecoinTx;
  }
}
function getChancecoinTxs(address) {
  var txs = getTransactions(address);
  var chancecoinTxs = [];
  for (i in txs) {
    var tx = txs[i];
    chancecoinTx = getChancecoinTx(tx.txid);
    if (chancecoinTx) {
      chancecoinTxs.push(chancecoinTx);
    }
  }
  return chancecoinTxs;
}
function decodeChancecoinTx(chancecoinTx) {
  var tx = chancecoinTx["tx"];
  var txid = tx.txid;
  if (CACHE_decodeChancecoinTx[txid]) {
    return CACHE_decodeChancecoinTx[txid];
  } else {
    var chancecoinTxDecoded = chancecoinTx;
    var messageType = chancecoinTx["messageType"];
    var message = chancecoinTx["message"];
    jsp = new JSPack();
    var source = null;
    for (i in tx.vin) {
      if (tx.vin[i].addr && source==null) {
        source = tx.vin[i].addr;
      }
    }
    var d = new XDate(tx.blocktime*1000);
    if (!tx.blocktime) {
      d = new XDate();
    }
    var blockTime = d.toString("yyyy-MM-dd HH:mm:ss");
    if (messageType==ID_ROLL && (message.length==LENGTH_ROLL || message.length==LENGTH_ROLL2)) {
      var txhash = createHexString(message.slice(0,32));
      var roll = jsp.Unpack("32sd",message);
      roll = roll[1];
      chancecoinTxDecoded["type"] = "roll";
      chancecoinTxDecoded["details"] = {"txid": txhash, "roll": roll};
    } else if (messageType==ID_DICE && message.length==LENGTH_DICE) {
      var a = jsp.Unpack(">Qdd", message);
      var bet = a[0]/UNIT;
      var chance = a[1];
      var payout = a[2];
      if (bet < getBalance(source, "CHA")) {
        chancecoinTxDecoded["type"] = "bet_dice";
        chancecoinTxDecoded["details"] = {"source": source, "block_time": blockTime, "bet": bet, "chance": chance, "payout": payout, "resolved": false, "roll": null, "profit": 0};
      }
    } else if (messageType==ID_POKER && message.length==LENGTH_POKER) {
      var a = jsp.Unpack(">Q9h4x", message);
      var bet = a[0]/UNIT;
      var cards = a.slice(1,10).map(function(x) { return getCard(x); }).join(" ");
      var chance = chanceOfWinning(cards)*100;
      var payout = 100/chance*(1-HOUSE_EDGE);
      if (bet < getBalance(source, "CHA")) {
        chancecoinTxDecoded["type"] = "bet_poker";
        chancecoinTxDecoded["details"] = {"source": source, "block_time": blockTime, "bet": bet, "chance": chance, "payout": payout, "resolved": false, "roll": null, "profit": 0, "cards": cards, "cards_result": false};
      }
    }
    CACHE_decodeChancecoinTx[txid] = chancecoinTxDecoded;
    return chancecoinTxDecoded;
  }
}

function getBets(address) {
  if (address == "") {
    address = FEE_ADDRESS;
  }
  var betObjects = [];
  var chancecoinTxs = getChancecoinTxs(address);
  for (i in chancecoinTxs) {
    var chancecoinTx = chancecoinTxs[i];
    var chancecoinTxDecoded = decodeChancecoinTx(chancecoinTx);
    if (chancecoinTxDecoded != null && (chancecoinTxDecoded["type"] == "bet_dice" || chancecoinTxDecoded["type"] == "bet_poker")) {
      if (!chancecoinTxDecoded["details"]["resolved"]) {
        chancecoinTxDecoded = resolveBet(chancecoinTxDecoded);
        if (chancecoinTxDecoded["details"]["resolved"]) {
          //once we resolve the first bet, we're good to go
          $("#loadingDiv").hide();
          $("#bodyDiv").show();
        }
      }
      betObjects.push(chancecoinTxDecoded);
    }
  }
  return betObjects;
}

function factorial (n){
  if (n==0 || n==1){
    return 1;
  }
  return factorial(n-1)*n;
}

function getDeck() {
  var deck = [];
  for (var c = 0; c<=12; c++) {
    for (var s = 0; s<=3; s++) {
      deck.push(getCard(s*13+c));
    }
  }
  return deck;
}

function shuffleAndDeal(roll, removedCards, nDeal) {
  var deck = getDeck();
  var shuffled = [];
  for (i in removedCards) {
    var position = $.inArray(removedCards[i], deck);
    if (position>-1) {
      deck.splice(position,1);
    }
  }

  var nCards = deck.length;
  var nToDeal = nDeal;
  var nLeftToDeal = nDeal;
  var nMax = factorial(nCards) / factorial(nCards - nToDeal);
  var n = Math.floor(nMax * roll);

  while (nLeftToDeal > 0) {
    var divider = factorial(nCards-1-(nToDeal-nLeftToDeal))/factorial(nCards-nToDeal);
    var digit = Math.floor(n / divider);
    var card = deck.splice(digit,1);
    shuffled.push(card[0]);
    n = n % divider;
    nLeftToDeal = nLeftToDeal - 1;
  }
	return shuffled;
}

function didWin(cards) {
  if (!(cards instanceof Array)) {
    cards = cards.split(" ");
  }
  var playerA = cards.slice(0,2).join(" ");
  var board = cards.slice(2,7).join(" ");
  var playerB = cards.slice(7,9).join(" ");
  var results = getPokerResults(playerA, playerB, board);
  if (results.winner == "playerA") {
    return true;
  } else {
    return false;
  }
}

function didWinResult(cards) {
  if (!(cards instanceof Array)) {
    cards = cards.split(" ");
  }
  var playerA = cards.slice(0,2).join(" ");
  var board = cards.slice(2,7).join(" ");
  var playerB = cards.slice(7,9).join(" ");
  var results = getPokerResults(playerA, playerB, board);
  return results;
}

function chanceOfWinning(cards) {
  if (!(cards instanceof Array)) {
    cards = cards.split(" ");
  }
  var deck = getDeck();
  var k = 0;
  for (i in cards) {
    var card = cards[i];
    var position = $.inArray(card, deck);
    if (card == "??") {
      k = k + 1;
    }
    if (position>-1) {
      deck.splice(position,1);
    }
  }
  var combinations = getCombinations(k, deck.length);
  var denominator = 0;
  var numerator = 0;
  for (i in combinations) {
    denominator ++;
    var combination = combinations[i];
    var cardsFilledIn = cards.slice();
    for (j in cardsFilledIn) {
      var card = cardsFilledIn[j];
      if (card == "??") {
        var popped = combination.pop();
        cardsFilledIn[j] = deck[popped];
      }
    }
    if (didWin(cardsFilledIn)) {
      numerator ++;
    }
  }
  return numerator / denominator;
}

function resolveBet(chancecoinTxDecoded) {
  var betObject = chancecoinTxDecoded["details"];

  var chaSupply = CHASupplyForBetting();
  if (!(chaSupply > 0)) {
    return chancecoinTxDecoded;
  }

  var earlierBetIsUnresolved = false; //TODO
  if (earlierBetIsUnresolved) {
    //if an earlier bet by the same address is still unresolved, don't resolve this one yet
    return chancecoinTxDecoded;
  }

  var couldWin = 0; //TODO: the total amount of CHA that could be won in this block

  var roll = null;
  var rollA = null;
  var rollB = null;
  var rollC = 0;

  if (couldWin > 20000) {
    //TODO: must use lottery numbers to resolve bet
    return chancecoinTxDecoded;
  } else {
    rollA = 0.0;
  }

  var tx = chancecoinTxDecoded["tx"];
  var blockHash = tx.blockhash;
  var txHash = tx.txid;
  if (blockHash) {
    rollC = (new BigInteger(blockHash,16)).mod(new BigInteger('1000000000')).intValue()/1000000000;
  }
  if (rollA != null && blockHash) {
    rollB = (new BigInteger(txHash.substring(10,txHash.length),16)).mod(new BigInteger('1000000000')).intValue()/1000000000;
    roll = ((rollA + rollB + rollC) % 1) * 100;
  }

  tx = getTx(tx.txid, true);
  var foundRoll = false;
  for (i in tx.vout) {
    var vout = tx.vout[i];
    if (!foundRoll && vout["spentTxId"]) {
      var spentTxId = vout["spentTxId"];
      if (spentTxId) {
        var decodedChancecoinTx = decodeChancecoinTx(getChancecoinTx(spentTxId));
        if (decodedChancecoinTx && decodedChancecoinTx["type"] == "roll") {
          var rollObject = decodedChancecoinTx["details"];
          foundRoll = true;
          roll = rollObject["roll"]*100;
        }
      }
    }
  }

  if (roll != null) {
    betObject["resolved"] = "true";
    var bet = betObject["bet"];
    var chance = betObject["chance"];
    var payout = betObject["payout"];
    getBalances();
    if (betObject["cards"]) {
      //poker bet
      var cards = betObject["cards"].split(" ");
      var removedCards = [];
      var dealtSoFar = [];
      for (i in cards) {
        var card = cards[i];
        if (card != "??") {
          removedCards.push(card);
        }
        dealtSoFar.push(card);
      }
      var deal = shuffleAndDeal(roll / 100, removedCards, dealtSoFar.length-removedCards.length);
      for (i = 0; i<dealtSoFar.length; i++) {
        if (dealtSoFar[i] == "??") {
          dealtSoFar[i] = deal.splice(0,1)[0];
        }
      }
      betObject["cards"] = dealtSoFar.join(" ");
      var results = didWinResult(dealtSoFar);
      betObject["cards_result"] = results.winningHand+" vs "+results.losingHand;
      if (results.winner == "playerA") {
        betObject["profit"] = bet*(payout-1)*chaSupply/(chaSupply-bet*payout);
      } else {
        betObject["profit"] = -bet;
      }
      if (!tx.blockhash || getBTCBlockHeight(tx.blockhash)>BALANCES.height) {
        BALANCES.balances[betObject["source"]] += betObject["profit"];
      }
    } else {
      //dice bet
      betObject["roll"] = roll;
      if (roll < chance) {
        betObject["profit"] = bet*(payout-1)*chaSupply/(chaSupply-bet*payout);
      } else {
        betObject["profit"] = -bet;
      }
      if (!tx.blockhash || getBTCBlockHeight(tx.blockhash)>BALANCES.height) {
        BALANCES.balances[betObject["source"]] += betObject["profit"];
      }
    }
  }
  chancecoinTxDecoded["details"] = betObject;
  CACHE_decodeChancecoinTx[chancecoinTxDecoded["tx"].txid] = chancecoinTxDecoded;
  return chancecoinTxDecoded;
}

function cardToNumber(card) {
  var rank = null;
  var suit = null;
  switch (card.charAt(1)) {
    case "C": suit=0; break;
    case "D": suit=1; break;
    case "H": suit=2; break;
    case "S": suit=3; break;
  }
  switch (card.charAt(0)) {
    case "2": rank=0; break;
    case "3": rank=1; break;
    case "4": rank=2; break;
    case "5": rank=3; break;
    case "6": rank=4; break;
    case "7": rank=5; break;
    case "8": rank=6; break;
    case "9": rank=7; break;
    case "T": rank=8; break;
    case "J": rank=9; break;
    case "Q": rank=10; break;
    case "K": rank=11; break;
    case "A": rank=12; break;
  }
  if (rank != null && suit != null) {
    return rank+suit*13;
  } else {
    return -1;
  }
}

function getCard(card) {
  var rank;
  var suit;
  switch (Math.floor(card / 13)) {
    case 0: suit="C"; break;
    case 1: suit="D"; break;
    case 2: suit="H"; break;
    case 3: suit="S"; break;
  }
  switch (card % 13) {
    case 0: rank="2"; break;
    case 1: rank="3"; break;
    case 2: rank="4"; break;
    case 3: rank="5"; break;
    case 4: rank="6"; break;
    case 5: rank="7"; break;
    case 6: rank="8"; break;
    case 7: rank="9"; break;
    case 8: rank="T"; break;
    case 9: rank="J"; break;
    case 10: rank="Q"; break;
    case 11: rank="K"; break;
    case 12: rank="A"; break;
  }
  if (rank && suit) {
    return rank+suit;
  } else {
    return "??";
  }
}
function getCardRank(card) {
  if (card.charAt(0) == "T") {
    return 10;
  } else {
    return card.charAt(0);
  }
}
function getCardSuit(card) {
  if (card.charAt(1) == "D") {
    return "diams";
  } else if (card.charAt(1) == "H") {
    return "hearts";
  } else if (card.charAt(1) == "S") {
    return "spades";
  } else if (card.charAt(1) == "C") {
    return "clubs";
  }
}

function createCookie(name,value,days) {
  if (localStorage) {
    localStorage.setItem(name, value);
  } else {
    if (days) {
      var date = new Date();
      date.setTime(date.getTime()+(days*24*60*60*1000));
      var expires = "; expires="+date.toGMTString();
    }
    else var expires = "";
    document.cookie = name+"="+value+expires+"; path=/";
  }
}
function readCookie(name) {
  if (localStorage) {
    return localStorage.getItem(name);
  } else {
    var nameEQ = name + "=";
    var ca = document.cookie.split(';');
    for(var i=0;i < ca.length;i++) {
      var c = ca[i];
      while (c.charAt(0)==' ') c = c.substring(1,c.length);
      if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
    }
    return null;
  }
}
function eraseCookie(name) {
  if (localStorage) {
    localStorage.removeItem(name);
  } else {
    createCookie(name,"",-1);
  }
}

function showError(message) {
  showMessage(message, "error");
  //throw error;
}

function showMessage(message, type) {
  $("#ajax_info").hide();
  $("#ajax_error").hide();
  if (!type || (type && type == "info")) {
    $("#ajax_info_content").html(message);
    $("#ajax_info").show();
  }
  if (type && type == "error") {
    LOG.push(message);
    $("#log").html(LOG.join("<br /><br />"));
    $("#ajax_error_content").html(message);
    $("#ajax_error").show();
  }
  $("#ajax_message").show();
}

function hideMessage() {
  $("#ajax_message").hide();
  $("#ajax_info").hide();
  $("#ajax_error").hide();
}

function disableForm(formName) {
  $("#"+formName+" input").prop("disabled", true);
  $("#"+formName+" button").prop("disabled", true);
}
function enableForm(formName) {
  $("#"+formName+" input").prop("disabled", false);
  $("#"+formName+" button").prop("disabled", false);
}

function getBTCPrice() {
  if (CACHE_getBTCPrice) {
    return CACHE_getBTCPrice;
  } else {
    var url = CORS_PROXY + "blockchain.info/q/24hrprice";
    var price = 0;
    download(url, true, function(data) {
      var price = 0;
      if (data) {
        price = data*1;
        CACHE_getBTCPrice = price;
      }
    });
    return price;
  }
}

function getCHAPrice() {
  if (CACHE_getCHAPrice) {
    return CACHE_getCHAPrice;
  } else {
    var url = CORS_PROXY + "coinmarketcap.northpole.ro/api/v5/CHA.json";
    var price = 0;
    download(url, true, function(data) {
      var price = 0;
      if (data) {
        price = data.price.btc*1;
        CACHE_getCHAPrice = price;
      }
    });
    return price;
  }
}

function getCHASupply() {
  return CHASupplyForBetting();
}

function getBTCBlockHash() {
  var url = "https://insight.bitpay.com/api/status?q=getLastBlockHash";
  var blockHash = "";
  if (CACHE_getBTCBlockHash) {
    blockHash = CACHE_getBTCBlockHash;
  }
  download(url, false, function(data) {
    if (data) {
      CACHE_getBTCBlockHash = data.lastblockhash;
    }
  });
  return blockHash;
}

function getBlock(blockHash) {
  var url = "https://insight.bitpay.com/api/txs?block="+blockHash;
  var block = null;
  if (CACHE_getBlock[block]) {
    block = CACHE_getBlock[block];
  } else {
    download(url, true, function(data) {
      if (data) {
        CACHE_getBlock[block] = data;
      }
    });
  }
  return block;
}

function getNewTransactions() {
  var blocks = getBlocks();
  getBalances();
  if (blocks) {
    for (i in blocks) {
      var block = blocks[i];
      //TODO: check here to ensure the new block's height is the next block. we don't want to skip blocks.
      if (block.height > BALANCES.height && BALANCES.height>0) {
        var blockHeight = block.height;
        block = getBlock(block.hash);
        if (block) {
          for (i in block.txs) {
            var tx = block.txs[i];
            CACHE_getTx[tx.txid] = tx;
            var chancecoinTx = getChancecoinTx(tx.txid);
            if (chancecoinTx) {
              var chancecoinTxDecoded = decodeChancecoinTx(chancecoinTx);
              if (chancecoinTxDecoded != null) {
                if (chancecoinTxDecoded["type"] == "bet_dice" || chancecoinTxDecoded["type"] == "bet_poker") {
                  if (!chancecoinTxDecoded["details"]["resolved"]) {
                    resolveBet(chancecoinTxDecoded);
                  }
                }
              }
            }
          }
        }
        BALANCES.height = blockHeight;
      }
    }
  }
}

function getBlocks() {
  var url = CORS_PROXY + "blockchain.info/blocks/?format=json";
  var blocks = [];
  download(url, false, function(data) {
    if (data) {
      for (i in data.blocks) {
        var block = data.blocks[i];
        CACHE_getBTCBlockHeight[block.hash] = block.height;
      }
    }
  });
  for (hash in CACHE_getBTCBlockHeight) {
    blocks.push({hash: hash, height: CACHE_getBTCBlockHeight[hash]});
  }
  blocks.reverse();
  return blocks;
}

function getCHABlockHeight() {
  getBalances();
  var blockHeight = 0;
  if (BALANCES && BALANCES.height) {
    blockHeight = BALANCES.height;
  }
  return blockHeight;
}

function getBTCBlockHeight(hash) {
  if (!hash) {
    hash = getBTCBlockHash();
  }
  if (CACHE_getBTCBlockHeight[hash]) {
    return CACHE_getBTCBlockHeight[hash];
  } else {
    var url = "https://insight.bitpay.com/api/block/"+hash;
    var blockHeight = 0;
    CACHE_getBTCBlockHeight[hash] = blockHeight;
    if (hash) {
      download(url, false, function(data) {
        var blockHeight = 0;
        if (data) {
          blockHeight = data.height;
        }
        CACHE_getBTCBlockHeight[hash] = blockHeight;
      });
    }
    return blockHeight;
  }
}

function getVersion() {
  return "Lite 1.0";
}

function getCasinoInfo() {
  var address = ADDRESS;
  var chaSupply = getCHASupply();
  var blockHeightBTC = getBTCBlockHeight();
  var blockHeightCHA = getCHABlockHeight();
  var version = getVersion();
  var chaPrice = getCHAPrice();
  var btcPrice = getBTCPrice();

  var addressInfos = [];
  var addresses = JSON.parse(readCookie("addresses"));

  if (addresses) {
    var i = addresses.indexOf(address);
    if (i>=0) { //delete the selected address so we can bring it to the top
      addresses.splice(i, 1);
    }
  }
  if (address) {
    addressInfos.push({address: address, balanceCHA: getBalance(address, "CHA")});
  }
  for (i in addresses) {
    addressInfos.push({address: addresses[i], balanceCHA: getBalance(addresses[i], "CHA")});
  }
  updateAddressDropDown(addressInfos);

  $("#cha_price_dollar").html("1 CHA = $" + (btcPrice * chaPrice).toFixed(2));
  $("#cha_supply").html(chaSupply.toLocaleString());
  $("#cha_price").html(chaPrice.toFixed(5) + " BTC");
  $("#btc_price").html("$"+btcPrice.toFixed(2));
  $("#market_cap").html("$"+(chaSupply * btcPrice * chaPrice).toLocaleString());

  $("#cha_over_btc_blocks").html(blockHeightCHA.toLocaleString() + " / " + blockHeightBTC.toLocaleString());
  $("#cha_blocks").html(blockHeightCHA.toLocaleString());
  $("#btc_blocks").html(blockHeightBTC.toLocaleString());
  $("#version").html(version);

  $("#recent_bets_content").html(getBetTableHtml(getBets("")));
  if (address) {
    $("#my_bets_content").html(getBetTableHtml(getBets(address)));
  }
  $("ul#addresses").attr("class","dropdown-menu");
}

function getBetTableHtml(decodedChancecoinTxs) {
  var html = "";
  html += '<table class="table table-striped">';
  html += '<thead>';
  html += '<tr>';
  html += '<th>Source address</th>';
  html += '<th>Time</th>';
  html += '<th>Bet size</th>';
  html += '<th>Chance to win / payout multiplier</th>';
  html += '<th>Result</th>';
  html += '<th>Profit</th>';
  html += '</tr>';
  html += '</thead>';
  html += '<tbody>';
  for (i in decodedChancecoinTxs) {
    var betInfo = decodedChancecoinTxs[i]["details"];
    var tx = decodedChancecoinTxs[i]["tx"];
    html += '<tr>';
    html += '<td><a href="http://blockchain.info/tx/'+tx["txid"]+'">'+betInfo["source"].substring(0,6)+'...</a></td>';
    html += '<td>'+betInfo["block_time"]+'</td>';
    html += '<td>'+betInfo["bet"].toPrecision(3)+' CHA</td>';
    html += '<td>'+parseFloat(betInfo["chance"].toPrecision(3))+'%'+' / '+parseFloat(betInfo["payout"].toPrecision(3))+'X</td>';
    if (betInfo["cards"]) {
      html += '<td>';
      var cardArray = betInfo["cards"].split(" ");
      for (var cardIndex = 0; cardIndex < cardArray.length; cardIndex++) {
          if (cardIndex == 0) {
              html += '<div style="float: left; padding-top: 1.25em; padding-right: 2.5em;">Player</div>';
          }
          if (cardIndex == 7) {
              html += '<div style="float: left; padding-top: 1.25em; padding-right: 1em;">Opponent</div>';
          }
          if (cardArray[cardIndex] == "??") {
              html += '<div class="card back">*</div>';
          } else {
              html += '<div class="card rank-'+getCardRank(cardArray[cardIndex])+' '+getCardSuit(cardArray[cardIndex])+'"">';
              html += '<span class="rank">'+getCardRank(cardArray[cardIndex])+'</span>';
              html += '<span class="suit">&'+getCardSuit(cardArray[cardIndex])+';</span>';
              html += '</div>';
          }
          if (cardIndex==1 || cardIndex==6) {
              html += '<div style="clear: both;"></div>';
          }
      }
      if (betInfo["cards_result"]) {
          html += '<p>'+betInfo['cards_result']+'</p>';
      }
      html += '</td>';
    } else {
      html += '<td><img src="'+HOME+'/images/dice.png" style="height: 25px; display: inline;" />';
      if (betInfo["resolved"]) {
        html += parseFloat(betInfo["roll"].toPrecision(5));
      } else {
        html += '?';
      }
      html += '</td>';
    }
    html += '<td>';
    if (betInfo["resolved"] && betInfo["resolved"]=="true") {
      html += betInfo["profit"].toPrecision(3)+" CHA";
    } else {
      html += '<img src="'+HOME+'/images/ajax-loader.gif" /></td>';
    }
    html += '</tr>';
  }
  html += '</tbody>';
  html += '</table>';
  return html;
}
function CHASupplyForBetting() {
  getBalances();
  var supply = 0;
  for (address in BALANCES.balances) {
    var balance = BALANCES.balances[address];
    supply = supply + balance;
  }
  return supply;
}
function updateBalances() {
  var url = "https://api.github.com/repos/chancecoin/chancecoinj/contents/balances.txt";
  download(url, false, function(data) {
    var balances = null;
    if (data) {
      balances = JSON.parse(Base64.decode(data.content));
      BALANCES = balances;
      createCookie("balances", JSON.stringify(balances), 999999);
    }
  });
}
function getBalances() {
  if (BALANCES) {
    return BALANCES;
  } else {
    var balancesCookie = readCookie("balances");
    //var balancesCookie = null;
    if (balancesCookie) {
      BALANCES = JSON.parse(balancesCookie);
    } else {
      BALANCES = {balances: {}, height: 0};
    }
    updateBalances();
    return BALANCES;
  }
}
function getBalance(address, asset) {
  getBalances();
  var balance = 0;
  if (asset == "CHA") {
    if (BALANCES.balances[address]) {
      balance = BALANCES.balances[address];
    }
  } else if (asset == "BTC") {
    var url = "http://api.bitwatch.co/getbalance/"+address+"?minconf=1&maxreqsigs=1";
    var data = download(url);
    if (data) {
      balance = data.result;
    }
  }
  return balance;
}

function download(url, cache, callback) {
  var result = null;
  if (!cache) {
    cache = false;
  }
  var async = false;
  if (!callback) {
    $.ajax({
      url: url,
      cache: cache,
      async: false
    }).done(function( data ) {
      result = data;
    }).fail( function(xhr, textStatus, errorThrown) {
      LOG.push("Failed to download "+url);
    });
  } else {
    $.ajax({
      url: url,
      cache: cache,
      async: true
    }).done(
      callback
    ).fail( function(xhr, textStatus, errorThrown) {
      LOG.push("Failed to download "+url);
    });
  }
  //console.log("Downloading "+url);
  return result;
}

function getPokerCardsHtml(cards, chance) {
  var html = '';
  html += '<div class="form-group">';
  html += '  <div class="input-group">';
  html += '    <span class="help-block">You</span>';
  html += '      <input type="hidden" name="card1" value="'+cards[0]+'" />';
  html += '      <div class="card rank-'+getCardRank(cards[0])+' '+getCardSuit(cards[0])+'">';
  html += '          <span class="rank">'+getCardRank(cards[0])+'</span>';
  html += '          <span class="suit">&'+getCardSuit(cards[0])+';</span>';
  html += '      </div>';
  html += '  </div>';
  html += '</div>';
  html += '<div class="form-group">';
  html += '  <div class="input-group">';
  html += '    <span class="help-block">&nbsp;</span>';
  html += '      <input type="hidden" name="card2" value="'+cards[1]+'" />';
  html += '      <div class="card rank-'+getCardRank(cards[1])+' '+getCardSuit(cards[1])+'">';
  html += '          <span class="rank">'+getCardRank(cards[1])+'</span>';
  html += '          <span class="suit">&'+getCardSuit(cards[1])+';</span>';
  html += '      </div>';
  html += '  </div>';
  html += '</div>';
  html += '<div class="form-group" style="clear: both; width: 4em;">';
  html += '</div>';
  html += '<div class="form-group">';
  html += '  <div class="input-group">';
  html += '    <span class="help-block">Board</span>';
  html += '      <input type="hidden" name="card3" value="'+cards[2]+'" />';
  html += '      <div class="card rank-'+getCardRank(cards[2])+' '+getCardSuit(cards[2])+'">';
  html += '          <span class="rank">'+getCardRank(cards[2])+'</span>';
  html += '          <span class="suit">&'+getCardSuit(cards[2])+';</span>';
  html += '      </div>';
  html += '  </div>';
  html += '</div>';
  html += '<div class="form-group">';
  html += '  <div class="input-group">';
  html += '    <span class="help-block">&nbsp;</span>';
  html += '      <input type="hidden" name="card4" value="'+cards[3]+'" />';
  html += '      <div class="card rank-'+getCardRank(cards[3])+' '+getCardSuit(cards[3])+'">';
  html += '          <span class="rank">'+getCardRank(cards[3])+'</span>';
  html += '          <span class="suit">&'+getCardSuit(cards[3])+';</span>';
  html += '      </div>';
  html += '  </div>';
  html += '</div>';
  html += '<div class="form-group">';
  html += '  <div class="input-group">';
  html += '    <span class="help-block">&nbsp;</span>';
  html += '      <input type="hidden" name="card5" value="'+cards[4]+'" />';
  html += '      <div class="card rank-'+getCardRank(cards[4])+' '+getCardSuit(cards[4])+'">';
  html += '          <span class="rank">'+getCardRank(cards[4])+'</span>';
  html += '          <span class="suit">&'+getCardSuit(cards[4])+';</span>';
  html += '      </div>';
  html += '  </div>';
  html += '</div>';
  html += '<div class="form-group">';
  html += '  <div class="input-group">';
  html += '    <span class="help-block">&nbsp;</span>';
  html += '      <input type="hidden" name="card6" value="??" />';
  html += '      <div class="card back">*</div>';
  html += '  </div>';
  html += '</div>';
  html += '<div class="form-group">';
  html += '  <div class="input-group">';
  html += '    <span class="help-block">&nbsp;</span>';
  html += '      <input type="hidden" name="card7" value="??" />';
  html += '      <div class="card back">*</div>';
  html += '  </div>';
  html += '</div>';
  html += '<div class="form-group" style="clear: both; width: 4em;">';
  html += '</div>';
  html += '<div class="form-group">';
  html += '  <div class="input-group">';
  html += '    <span class="help-block">Opponent</span>';
  html += '      <input type="hidden" name="card8" value="'+cards[7]+'" />';
  html += '      <div class="card rank-'+getCardRank(cards[7])+' '+getCardSuit(cards[7])+'">';
  html += '          <span class="rank">'+getCardRank(cards[7])+'</span>';
  html += '          <span class="suit">&'+getCardSuit(cards[7])+';</span>';
  html += '      </div>';
  html += '  </div>';
  html += '</div>';
  html += '<div class="form-group">';
  html += '  <div class="input-group">';
  html += '    <span class="help-block">&nbsp;</span>';
  html += '      <input type="hidden" name="card9" value="'+cards[8]+'" />';
  html += '      <div class="card rank-'+getCardRank(cards[8])+' '+getCardSuit(cards[8])+'">';
  html += '          <span class="rank">'+getCardRank(cards[8])+'</span>';
  html += '          <span class="suit">&'+getCardSuit(cards[8])+';</span>';
  html += '      </div>';
  html += '  </div>';
  html += '</div>';
  html += '<div class="form-group" style="clear: both; width: 4em;">';
  html += '</div>';
  html += '<p>Chance of winning: '+chance.toPrecision(5)+'%</p>';
  return html;
}

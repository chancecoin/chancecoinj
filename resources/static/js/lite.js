var FEEADDRESS = "1CHACHAGuuxTr8Yo9b9SQmUGLg9X5iSeKX";

$(window).on('load', function () {
    $('.selectpicker').selectpicker({
        'selectedText': 'cat'
    });
    importPrivateKey();
});

$(document).ready(function() {
  if(location.hash) {
    $('a[href=' + location.hash + ']').tab('show');
  }
  $(document.body).on("click", "a[data-toggle]", function(event) {
    location.hash = this.getAttribute("href");
  });
  if($("#num_unresolved_bets").html() > 0) {
    setInterval(function(){updateUnresolvedBets();}, 5000);
  }
  setInterval(function(){update();}, 5000);
  getPendingBets();
});

$(window).on('popstate', function() {
  var anchor = location.hash || $("a[data-toggle=tab]").first().attr("href");
  $('a[href=' + anchor + ']').tab('show');
});

function updateChatStatus() {
  $.ajax({
    url: "/chat_status_update?chat_open="+$('#chat_open').html(),
    type: 'GET',
    success: function(data) {
    }
  });
}

function updateUnresolvedBets() {
  $.ajax({
    type: "POST",
    url: "/update_unresolved_bets",
    success: function(response) {
      if (response < $("#num_unresolved_bets").html()) {
        window.location.reload(true);
      } else {
        $("#num_unresolved_bets").html(response);
      }
    }
  });
}

function update() {
  //getPendingBets();
}

function importPrivateKey() {
  var privateKey = $( "input[name=privatekey]" ).val();
  if (privateKey) {
      try {
          var key = new Bitcoin.ECKey.fromWIF(privateKey);
          var address = key.pub.getAddress().toString();
          createCookie("private_key", privateKey, 999999);
          createCookie("address", address, 999999);
      } catch (e) {
      }
  } else {
      //eraseCookie("private_key");
      //eraseCookie("address");
  }
  if (readCookie("address")) {
      try {
          var address = readCookie("address");
          //$("#addresses").empty();
          $("#addresses").append('<li><a href="?address='+address+'"><strong>'+address+'</strong> <span class="badge"> CHA</span></a></li>');
          $("#address").html(address.substring(0,6)+'... <b class="caret"></b>');
      } catch (e) {
      }
  }
}
function getUnspents(address) {
    var url = "http://api.bitwatch.co/listunspent/"+address+"?verbose=1&minconf=0";
    var unspents = [];
    console.log(url);
    $.ajax({
      url: url,
      cache: false,
      async: false
    }).done(function( data ) {
      $.each(data.result, function(i,result){
          unspents.push(result);
      });
    });
    return unspents;
}
function processBet(formName) {
    var bet = $( "input[name=bet]" ).val();
    var resolution = $( "input[name=resolution]" ).val();
    var asset = $( "select[name=asset]" ).val();
    var chance = $( "input[name=chance]" ).val();
    var payout = $( "input[name=payout]" ).val();
    var address = readCookie("address");
    if (bet && resolution && asset && chance && payout && address) {
        var unspents = getUnspents(address);
        for (i in unspents) {
            var tx = getTx(unspents[i].txid);
            console.log(tx);
        }
        //TODO
    }
}
function getTx(txid) {
    var url = "https://insight.bitpay.com/api/tx/"+txid;
    var tx = {};
    $.ajax({
      url: url,
      cache: false,
      async: false
    }).done(function( data ) {
      tx = data;
    });
    return tx;
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
function getPendingBets() {
  console.log("getting unspents");
  var unspents = getUnspents(FEEADDRESS);
  for (i in unspents) {
    var unspent = unspents[i];
    if (unspent.confirmations==0) {
      var tx = getTx(unspent.txid);
      var data = [];
      for (vout in tx.vout) {
        var vout = tx.vout[vout];
        var asm = vout.scriptPubKey.asm.split(' ');
        //console.log(asm);
        if (asm.length==2 && asm[0]=="OP_RETURN") {
          data = data.concat(parseHexString(asm[1]));
        } else if (asm.length>=5 && asm[0]=='1' && asm[3]=='2' && asm[4]=='OP_CHECKMULTISIG') {
          var data_pubkey = parseHexString(asm[2]);
          var data_chunk_length = data_pubkey[0];
          data = data.concat(data_pubkey);
          data = data.concat(data_pubkey.slice(1,data_chunk_length+1));
        }
      }
      var message = getMessage(data);
      var messageType = getMessageType(data);
      console.log(messageType);
    }
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
    if (days) {
        var date = new Date();
        date.setTime(date.getTime()+(days*24*60*60*1000));
        var expires = "; expires="+date.toGMTString();
    }
    else var expires = "";
    document.cookie = name+"="+value+expires+"; path=/";
}
function readCookie(name) {
    var nameEQ = name + "=";
    var ca = document.cookie.split(';');
    for(var i=0;i < ca.length;i++) {
        var c = ca[i];
        while (c.charAt(0)==' ') c = c.substring(1,c.length);
        if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
    }
    return null;
}
function eraseCookie(name) {
    createCookie(name,"",-1);
}
function getCasinoInfo() {
  $.ajax({
    type: "GET",
    url: "http://0.0.0.0:8080/get_casino_info",
    crossDomain: true,
    success: function(response) {
      var responseObj = jQuery.parseJSON(response);
      var betInfo;
      var html = "";
      html += "<table class='table table-striped'>"
      html += "<thead>";
      html +=	"<tr>";
      html +=	"<th>Source address</th>";
      html +=	"<th>Time</th>";
      html +=	"<th>Bet size</th>";
      html +=	"<th>Chance to win / payout multiplier</th>";
      html +=	"<th>Result</th>";
      html +=	"<th>Profit</th>";
      html +=	"</tr>";
      html +=	"</thead>";
      html += "<tbody>";

       for (betInfo in responseObj.bets) {
        html += "<tr>";
        html += "<td>"+betInfo["source"];//.substring(0,6)+"...</td>";
        html += "<td>"+betInfo["block_time"]+"</td>";
        html += "<td>"+betInfo["bet"]+" CHA</td>";
        //html += "<td>"+parseFloat(betInfo["chance"].toPrecision(3))+"%"+" / "+parseFloat(betInfo["payout"].toPrecision(3))+"X</td>";
        html += "<td>"+parseFloat(betInfo["chance"])+"%"+" / "+parseFloat(betInfo["payout"])+"X</td>";

        if (betInfo["cards"]) {
          html += "<td>";
          var cardArray = betInfo["cards"].split(" ");
          for (var cardIndex = 0; cardIndex < cardArray.length; cardIndex++) {
              if (cardIndex == 0) {
                  html += "<div style='float: left; padding-top: 1.25em; padding-right: 2.5em;'>Player</div>";
              }
              if (cardIndex == 7) {
                  html += "<div style='float: left; padding-top: 1.25em; padding-right: 1em;'>Opponent</div>";
              }
              if (cardArray[cardIndex] == "??") {
                  html += "<div class='card back'>*</div>";
              } else {
                  html += "<div class='card rank-"+getCardRank(cardArray[cardIndex])+" "+getCardSuit(cardArray[cardIndex])+"'>";
                  html += "<span class='rank'>"+getCardRank(cardArray[cardIndex])+"</span>";
                  html +=	"<span class='suit'>&"+getCardSuit(cardArray[cardIndex])+";</span>";
                  html += "</div>";
              }
              if (cardIndex==1 || cardIndex==6) {
                  html += "<div style='clear: both;'></div>";
              }
          }
          if (betInfo["cards_result"]) {
              html += "<p>"+betInfo['cards_result']+"</p>";
          }
          html += "</td>";
        } else {
          html += "<td><img src='http://chancecoin.com/images/dice.png' style='height: 25px; display: inline;' />";
          if (betInfo["resolved"]) {
            html += betInfo["roll"];
          } else {
            html += "?";
          }
          html += "</td>";
        }
        html += "<td>";
        if (betInfo["resolved"] && betInfo["resolved"]=="true") {
          html += betInfo["profit"]+" CHA";
        } else {
          html += "<img src='http://chancecoin.com/images/ajax-loader.gif' /></td>";
        }
        html += "</tr>";
      }
      html += "</tbody>";
      html += "</table>";

      $("#bets_content").html(html);

      $("#cha_price_dollar").html("1 CHA = $" + (responseObj.price_BTC *responseObj.price_CHA).toFixed(2));
      $("#cha_supply").html(responseObj.supply.toLocaleString());
      $("#cha_price").html(responseObj.price_CHA.toFixed(4) + " BTC");
      $("#btc_price").html("$"+responseObj.price_BTC.toFixed(2));
      $("#market_cap").html("$"+(responseObj.supply * responseObj.price_BTC * responseObj.price_CHA).toLocaleString());

      var currentChaBlocks;
      if (responseObj.parsing) {
        currentChaBlocks = responseObj.parsing;
      } else {
        currentChaBlocks = responseObj.blocksCHA;
      }
      $("#cha_over_btc_blocks").html(currentChaBlocks.toLocaleString() + " / " + responseObj.blocksBTC.toLocaleString());
      $("#cha_blocks").html(currentChaBlocks.toLocaleString());
      $("#btc_blocks").html(responseObj.blocksBTC.toLocaleString());
      $("#version").html(responseObj.version);
    }
  });
}

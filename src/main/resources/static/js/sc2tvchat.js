var urlRegex = new RegExp('(https?:\/\/[^\\s\/$.?#].[^\\s,]*)', 'gi');
var nicknameBoldPattern = new RegExp( '\\[b\\]([\\s\\S]+?)\\[/b\\]', 'gi');

function urlReplace(inputText) {

    inputText = inputText.replace(urlRegex, function(url) {
        if(url.length > 60) {
            return '<a href="' + url + '" target="_blank">' + url.substr(0, 45) + '...' + url.slice(-15) + '</a>';
        }
        return '<a href="' + url + '" target="_blank">' + url + '</a>';
    });

    inputText = inputText.replace(nicknameBoldPattern, '<b>$1</b>' );

    return inputText;
}

var smiles;
var req = new XMLHttpRequest();
req.open('POST', 'https://funstream.tv/api/smile', false);

req.send();

if(req.status == 200) {
    smiles = JSON.parse(req.responseText);
}

var smilesCount = smiles.length;
var smileHtmlReplacement = [];
for( i = 0; i < smilesCount; i++ ) {
    smileHtmlReplacement[i] =
        '<img src="' + smiles[i].url +
        '" width="' + smiles[i].width +
        '" height="' + smiles[i].height +
        '" class="chat-smile"/>';
}

function processReplaces( message ) {

    var message = urlReplace( message );

    message = message.replace( /:([-a-z0-9]{2,}):/gi, function( match, code ) {
        var indexOfSmileWithThatCode = -1;
        for ( var i = 0; i < smilesCount; i++ ) {
            if ( smiles[i].code == code ) {
                indexOfSmileWithThatCode = i;
                break;
            }
        };

        var replace = '';
        if ( indexOfSmileWithThatCode != -1 ) {
            replace = smileHtmlReplacement[ indexOfSmileWithThatCode ];
        } else {
            replace = match;
        }

        return replace;
    });

    return message;
}

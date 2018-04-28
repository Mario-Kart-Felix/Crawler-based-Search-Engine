getSearchSuggestions=function(text){
    console.log("http://localhost:4567/"+text);
    $.get( "http://localhost:4567/"+text, function( succ ) {
        // $( ".result" ).html( data );
        alert( succ );
    });
};
sendDelayedKeyPress=function (key) {
    console.log("first");
    document.getElementById("searchbar").onkeypress=null;
    setTimeout(function() {
        console.log("here");
        console.log(key);
        getSearchSuggestions(document.getElementById("searchbar").value);
        document.getElementById("searchbar").onkeypress=sendDelayedKeyPress;
    }, 800);
};

document.getElementById("searchbar").onkeypress=sendDelayedKeyPress;
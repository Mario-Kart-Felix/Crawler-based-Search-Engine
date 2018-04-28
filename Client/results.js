
$.get("http://localhost:4567/"+localStorage.getItem("search-type")+"/" + localStorage.getItem("search"), function (succ) {
    // $( ".result" ).html( data );
    localStorage.setItem("data",succ);
    localStorage.setItem("location","0");
    console.log(JSON.parse(succ));
    results = document.getElementById("resultsPage");
    succ=JSON.parse(succ);
    $('#resultsPage').empty();
    let i=0;
    for(obj in succ){
        l="<div class=\"search-results\">"+
            "<h2><a href=\""+succ[obj].url+"\">"+succ[obj].title+"</a></h2>"+
        "<a><a style = \"color:green\">"+succ[obj].url+"</a></a>"+
        "<p class=\"summary\">"+succ[obj].snippet+"</p>"+
        "</div>";
        $('#resultsPage').append(l);
        i++;
        if(i>=10)
        {
            break;
        }

    }

});


sendDelayedKeyPress = function (key) {
    console.log("first");
    document.getElementById("search-bar").onkeypress = null;
    setTimeout(function () {
        console.log("here");
        console.log(key);
        getSearchSuggestions(document.getElementById("search-bar").value);
        document.getElementById("search-bar").onkeypress = sendDelayedKeyPress;
    }, 1000);
};
getSearchSuggestions = function (text) {
    console.log("http://localhost:4567/suggestion/" + text);
    $.get("http://localhost:4567/suggest/" + text, function (succ) {
        // $( ".result" ).html( data );
        $(function () {

                var obj = JSON.parse(succ);
                var sugg = obj.Suggestions;
                var availableTags = sugg;
                console.log(sugg);
                $("#searchbar").autocomplete({
                    source: availableTags
                });
            }
        );
        // alert( succ );
    });
};
submitSearch=function(text){
    localStorage.setItem("search-type","search");
    localStorage.setItem("search", document.getElementById("search-bar").value);
    window.location.href="results.html";
};
submitPSearch=function(text){
    localStorage.setItem("search-type","psearch");
    localStorage.setItem("search", document.getElementById("search-bar").value);
    window.location.href="results.html";
};
nextPage=function(){
    let succ=localStorage.getItem("data");

    succ=JSON.parse(succ);
    $('#resultsPage').empty();
    let start=parseInt(localStorage.getItem("location"));

    let i=start;
    let s=0;
    for(obj in succ){
        s++;
        if(s<start) {
            continue;
        }
        l="<div class=\"search-results\">"+
            "<h2><a href=\""+succ[obj].url+"\">"+succ[obj].title+"</a></h2>"+
            "<a><a style = \"color:green\">"+succ[obj].url+"</a></a>"+
            "<p class=\"summary\">"+succ[obj].snippet+"</p>"+
            "</div>";
        $('#resultsPage').append(l);
        i++;
        if(s>=start+10)
        {
            break;
        }

    }
    localStorage.setItem("location",(start+10).toString());
};
document.getElementById("search-bar").onkeypress=sendDelayedKeyPress;
document.getElementById("mag").onclick=submitSearch;
document.getElementById("mic").onclick=submitPSearch;
document.getElementById("nextPage").onclick=nextPage;
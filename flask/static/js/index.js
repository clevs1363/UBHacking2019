const socket = io.connect("http://localhost:8080", { transports: ['websocket'] });
var counter = 0;
var currentName = "";

socket.on('school_item', function(event) {
    var list = event.split("$");
    var newRow = document.createElement("tr");
    newRow.id = "dynamicNewRow" + counter;

    var name = document.createElement("td");
    var webLink = document.createElement("a");
    var linkText = document.createTextNode(list[0]);
    webLink.appendChild(linkText);
    webLink.href = "https://" + list[3];
    var websiteContent = name.appendChild(webLink);
    name.appendChild(websiteContent);
    newRow.appendChild(name);
    
    var city = document.createElement("td");
    var cityContent = document.createTextNode(list[1]); 
    city.appendChild(cityContent);
    newRow.appendChild(city);

    var state = document.createElement("td");
    var stateContent = document.createTextNode(list[2]); 
    state.appendChild(stateContent);
    newRow.appendChild(state);

    newRow.onclick = function() {
        var current = document.getElementById(this.id);
        var text = current.firstChild.firstChild.innerHTML;
        text = text.replace("&amp;", "&");
        window.location.href = 'schools/' + text;
    }
    var oldRow = document.getElementById("schoolTable");
    oldRow.appendChild(newRow);
    counter++;
});

function sendMessage() {
    socket.emit("chat_message", "hello");
}

function getSchools() {
    var elementExists = document.getElementById("dynamicNewRow");
    if (elementExists == null) {
        socket.emit("get_schools");
    }
}

function setColor() {
    var children = document.getElementById("tas").children;
    for (var child of children) {
        var element = child.firstChild;
        var num = element.innerHTML;
        if (num < 1.3) {
            element.style.backgroundColor = "#cc0000";
        } else if (num >= 1.3 && num < 2.5) {
            element.style.backgroundColor = "#ff9900";
        } else if (num >= 2.5 && num < 3.7) {
            element.style.backgroundColor = "#ffff00";
        } else if (num >= 3.7 && num <= 5.0) {
            element.style.backgroundColor = "#00ff00";
        } else if (num =='-') {
            element.style.backgroundColor = "C5C6C7";
        }
    }
    
}

function popModal(element) {
    var text = element.innerText;
    var name = text.substring(3)
    currentName = name;
    socket.emit("getTA", name);
    socket.on('TA', function(event) {
        var list = event.split("$");
        document.getElementById("modalLabel").innerHTML = name + ": " + list[2];
        var body = document.getElementById("modal-body");
        var TARating = document.getElementById("TARating")
        if (TARating == null) {
            var rating = document.createElement('h4');
            var ratingContent = document.createTextNode("Rating: " + list[1]);
            rating.id = "TARating";
            rating.appendChild(ratingContent);
            body.appendChild(rating);
        } else {
            TARating.innerHTML = ("Rating: " + list[1]);
        }

        var reviews = list[3].split(",");
        var rCont = document.getElementById("reviewCont");
        if (rCont == null) {
            var reviewCont = document.createElement('div');
            reviewCont.id = "reviewCont";
            for (var r of reviews) {
                var review = document.createElement('p');
                rContent = document.createTextNode(r);
                review.appendChild(rContent);
                reviewCont.appendChild(review);
            }
            body.appendChild(reviewCont);
        } else {
            while (rCont.firstChild) {
                rCont.removeChild(rCont.firstChild);
            }
            for (var r of reviews) {
                var review = document.createElement('p');
                rContent = document.createTextNode(r);
                review.appendChild(rContent);
                rCont.appendChild(review);
            }
        }
    });
}

function addTA() {
    var data = "";
    var name = document.getElementById("name").value;
    data = data + name + "$";
    var cl = document.getElementById("class").value;
    data = data + cl + "$";
    socket.emit("addTA", data); 
}

function addRating() {
    var data = "";
    data = data + currentName + "$";
    var rating = document.getElementById("exampleFormControlSelect1");
    data = data + rating + "$";
    var review = document.getElementById("exampleFormControlTextarea1");
    data = data + rating + "$";
    socket.emit("addRating", data);
}
$(document).ready(function () {
    const socket = io.connect("http://localhost:8080", { transports: ['websocket'] });
    var counter = 0;
    var currentName = "";
    var selectedTA;

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
            if (num > 0 && num < 1.3) {
                element.style.backgroundColor = "#cc0000";
            } else if (num >= 1.3 && num < 2.5) {
                element.style.backgroundColor = "#ff9900";
            } else if (num >= 2.5 && num < 3.7) {
                element.style.backgroundColor = "#ffff00";
            } else if (num >= 3.7 && num <= 5.0) {
                element.style.backgroundColor = "#00ff00";
            } else if (num == 0.0) {
                element.style.backgroundColor = "C5C6C7";
            }
        }
        
    }

    // Triggered when TA modal is clicked
    $(document).on("click", ".uniqueTAIdentifier", function(e) {
        var element = this;
        var text = element.innerText;
        var name = text.substring(3).trim();
        currentName = name;
        selectedTA = element;
        socket.emit("getTA", name);
        socket.on('TA', function(event) {
            var data = JSON.parse(event);
            console.log(data); 
            document.getElementById("modalLabel").innerHTML = "";
            document.getElementById("modalLabel").innerHTML = currentName + ": " + data.class;
            var body = document.getElementById("modal-body");
            var TARating = document.getElementById("TARating")
            if (TARating == null) {
                var rating = document.createElement('h4');
                if (data.overallRating == 0) { data.overallRating = "n/a";}
                var ratingContent = document.createTextNode("Rating: " + data.overallRating);
                rating.id = "TARating";
                rating.appendChild(ratingContent);
                body.appendChild(rating);
            } else {
                if (data.overallRating == 0 || data.overallRating == "n/a") { data.overallRating = "n/a"; }
                 else {TARating.innerHTML = ("Rating: " + (Math.round(10 * data.overallRating) / 10).toFixed(1));}
            }
            var rCont = document.getElementById("reviewCont");
            if (rCont == null) {
                var reviewContainer = document.createElement("div");
                reviewContainer.id = "reviewCont";
                body.appendChild(reviewContainer);
            } else {
                while (rCont.firstChild) {
                    rCont.removeChild(rCont.firstChild)
                }
                for (var r of data.reviews) {
                    if (r != "") {
                        var review = document.createElement('p');
                        review.className = "review";
                        rContent = document.createTextNode(r.charAt(0) + ": " + r.slice(1));
                        review.appendChild(rContent);
                        rCont.appendChild(review);
                    } 
                }
            }
            e.stopPropagation();
        });
    });

    $('#addTAButton').click(function() {
        var data = "";
        var nameData = document.getElementById("name").value;
        currentName = nameData;
        var classData = document.getElementById("class").value;
        document.getElementById("name").value = '';
        document.getElementById("class").value = '';
        var data = {
            name: nameData,
            class: classData
        };
        
        socket.emit("addTA", JSON.stringify(data)); 
        var TA = document.createElement('p');
        TA.className = "uniqueTAIdentifier";
        TA.setAttribute("data-toggle", "modal");
        TA.setAttribute("data-target", "#taModal");
        TA.innerHTML = "<span class=\"number mr-3\">n/a</span>" + " " + currentName;
        var div = document.getElementById("tas");
        div.appendChild(TA);
        setColor();
        document.getElementById("closeAddTA").click();
    });

    $('#addRatingButton').click(function() {
        var ratingData = document.getElementById("formRatingField").value;
        var reviewData = document.getElementById("formReviewField").value;
        document.getElementById("formRatingField").value = '';
        document.getElementById("formReviewField").value = '';
        var data = {
            name: currentName,
            rating: ratingData,
            review: reviewData
        };
        socket.emit("addRating", JSON.stringify(data));
        socket.on("getRating", function(rating) {
            selectedTA.firstChild.innerHTML = (Math.round(10 * rating) / 10).toFixed(1);
            setColor();
            document.getElementById("closeAddRating").click();
        });
    });
});
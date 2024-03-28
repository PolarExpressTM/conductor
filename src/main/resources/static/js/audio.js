updateTimeframe();

function updateTimeframe() {
    fetch(`/audio/timeframe`, {
        method: "GET",
        credentials: "include"
    })
        .then(value => {
            if (value.status !== 200) return;
            value.json().then(data => {
                console.log(data);
                document.getElementById('current-title').innerText = data.title;
                document.getElementById('current-author').innerText = data.author;
                document.getElementById('current-artwork').src = data.artworkUrl;
                document.getElementById('timeframe-slider').value = data.position;
                document.getElementById('timeframe-slider').max = data.duration;
                document.getElementById('timeframe-current').innerText = msToTime(data.position);
                document.getElementById('timeframe-max').innerText = msToTime(data.duration);
            }).catch(reason => console.log(reason));
        })
        .catch(reason => console.log(reason));
    setTimeout(() => updateTimeframe(), 900);
}

function changeTimeframe() {
    const timeframe = document.getElementById('timeframe-slider').value;
    fetch(`/audio/time/${timeframe}`, {
        method: "GET",
        credentials: "include"
    })
        .then(value => console.log(value))
        .catch(reason => console.log(reason));
}

// Source: https://stackoverflow.com/questions/9763441/milliseconds-to-time-in-javascript
function msToTime(s) {
    function pad(n, z) {
        z = z || 2;
        return ('00' + n).slice(-z);
    }
    const ms = s % 1000;
    s = (s - ms) / 1000;
    const secs = s % 60;
    s = (s - secs) / 60;
    const mins = s % 60;
    const hrs = (s - mins) / 60;
    return pad(hrs) + ':' + pad(mins) + ':' + pad(secs);
}

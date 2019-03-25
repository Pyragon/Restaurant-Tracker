function submitModal(e) {
    if (e.which == 13) {
        $(this).closest('.noty_bar').find('.submit-btn').click();
        return false;
    }
}
Object.size = function (obj) {
    var size = 0,
        key;
    for (key in obj) {
        if (obj.hasOwnProperty(key)) size++;
    }
    return size;
};
String.prototype.replaceAll = function (search, replacement) {
    var target = this;
    return target.replace(new RegExp(search, 'g'), replacement);
};

const CLOSE = 'Click to close search bar.';
const OPEN = 'Click to open search bar.';

var n = null;

function isEmpty(...args) {
    for (var i = 0; i < args.length; i++) {
        var val = args[i].toString();
        if (val.replace(/\s/g, "") == "")
            return true;
    }
    return false;
}

function getJSON(ret) {
    var data = JSON.parse(ret);
    if (!data) {
        sendAlert('Error with return from server!');
        console.error('Error with return from server!', ret);
        return null;
    }
    if (typeof data.success === 'undefined') {
        sendAlert('Session expired! Please reload the page to login again.');
        return null;
    }
    if (!data.success) {
        if (data.error !== '')
            sendAlert(data.error);
        return null;
    }
    if (data.errorMessage || data.message)
        sendAlert(data.errorMessage || data.message);
    return data;
}

function closeNoty(noty) {
    n = null
    noty.close();
}

function sendAlert(text) {
    var n = noty({
        text: text,
        layout: 'topRight',
        timeout: 5000,
        theme: 'cryogen'
    });
}

//static

$(document).ready(function () {
    $(document).on('keypress', '.modal-input', submitModal);
    $(document).on('keypress', '.report_line input', submitModal);
});
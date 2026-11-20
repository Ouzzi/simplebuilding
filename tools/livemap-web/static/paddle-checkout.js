/* Paddle Billing checkout overlay (N108 addendum 4). Values come from data attributes of #paddle-pay (no inline script).
   With ?_ptxn= in the address Paddle.js opens the checkout for that transaction by itself; the button opens it again. */
(function () {
  var d = document.getElementById('paddle-pay');
  if (!d || !window.Paddle) { return; }
  if (d.dataset.env === 'sandbox') { window.Paddle.Environment.set('sandbox'); }
  window.Paddle.Initialize({
    token: d.dataset.token,
    checkout: { settings: { displayMode: 'overlay', locale: d.dataset.locale, successUrl: d.dataset.success, allowLogout: false } },
    eventCallback: function (ev) {
      if (ev && ev.name === 'checkout.completed') { window.location.href = d.dataset.success; }
    }
  });
  var btn = document.getElementById('paddle-open');
  if (btn) {
    btn.addEventListener('click', function () {
      window.Paddle.Checkout.open({ transactionId: d.dataset.txn,
        settings: { displayMode: 'overlay', locale: d.dataset.locale, successUrl: d.dataset.success, allowLogout: false } });
    });
  }
}());

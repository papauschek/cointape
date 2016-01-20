
define(['app/libs'], function(libs) {

    // sophisticated react router
    var React = libs.React;
    var Router = libs.ReactRouter;
    var Route = Router.Route;
    var Link = Router.Link;
    var RouteHandler = Router.RouteHandler;
    var DefaultRoute = Router.DefaultRoute;
    var Redirect = Router.Redirect;

    var BaseApp = React.createClass({
        render: function () {
            return <RouteHandler />;
        }
    });

    var Header = React.createClass({
       render: function() {
           return  (<div>
               <div className="header">
                   <Link to="/">
                       <img alt="CoinTape.21.co" src="/assets/images/cointape_logo.png" />
                   </Link>
                   <div className="info">
                       Predicting Bitcoin transaction fees since 1759.
                   </div>
               </div>
               <div className="clearfix" />
           </div>);
       }
    });


    var Footer = React.createClass({
        render: function() {
            return  ( <div className="footer">

                <hr />
                <Link to="/">Home</Link> - <Link to="/api">Developer API</Link>

            </div>);
        }
    });


    var Index = React.createClass({
        getInitialState: function() {
            return {
                fees: [],
                message: '',
                medianTxSize: null
                //medianTxSize: 100
            };
        },
        componentDidMount: function() {
            this.refresh();
        },
        refresh: function() {
            $.get("/fees", function(result) {
                this.setState(result);
            }.bind(this)).fail(function(result) {
                if (result.responseText) {
                    this.setState({ message: result.responseText });
                } else {
                    this.setState({ message: 'CoinTape is currently offline and will be back online with data pulled directly from the Bitcoin network.'});
                }
            }.bind(this));
            setTimeout(this.refresh, 5000);
        },
        render: function () {

            var fastestFee = this.state.fees[this.state.bestIndex] || { maxFee: 10 };
            var txFee = (this.state.medianTxSize * fastestFee.maxFee);

            function numberWithCommas(x) {
                return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
            }

            var txFeeCommas = numberWithCommas(txFee);


            //var txFeeBits = Math.floor(txFee / 100);
            //var txFeeMBTC = txFeeBits / 1000;

            var renderFee = function(fee) {

                var percent = 100 * fee.count / this.state.maxCount;
                var memPercent = 100 * fee.memCount / this.state.maxMemCount;
                var maxDelay = fee.maxDelay >= 10000 ? "Inf" : fee.maxDelay;
                var maxMinutes = fee.maxMinutes >= 10000 ? "Inf" : fee.maxMinutes;

                return <tr key={fee.minFee} className={'speed' + fee.speed}>
                    <td className="labelcol bucket">{
                        (fee.minFee == fee.maxFee)
                            ? fee.maxFee
                            : <span><span className="min">{fee.minFee}-</span>{fee.maxFee}</span>
                    }</td>
                    <td className="count">
                        <div className="progress mem">
                            <div className="progress-bar" style={{ minWidth: 40, width: memPercent + '%' }}>
                                {fee.memCount}
                            </div>
                        </div>
                        <div className="progress day">
                            <div className="progress-bar" style={{ minWidth: 40, width: percent + '%' }}>
                                {fee.count}
                            </div>
                        </div>
                    </td>
                    <td className="labelcol delay">{
                        (fee.minDelay == fee.maxDelay)
                            ? maxDelay
                            : <span><span className="min">{fee.minDelay}-</span>{maxDelay}</span>
                    }</td>
                    <td className="labelcol delay">{
                        (fee.minMinutes == fee.maxMinutes)
                            ? maxMinutes
                            : <span><span className="min">{fee.minMinutes}-</span>{maxMinutes}</span>
                    }</td>
                </tr>;
            };

            return <div id="overview">
                <Header />
                <table className="table table-condensed table-hover">
                    <thead>
                    <tr>
                        <td>Fees <a href="#fees"><span className="glyphicon glyphicon-info-sign" /></a></td>
                        <td>
                                <span className="mem">Unconfirmed transactions
                                </span> / <span className="day">Transactions today</span>
                        </td>
                        <td>Delay <a href="#delay"><span className="glyphicon glyphicon-info-sign" /></a></td>
                        <td style={{textAlign: 'right'}}>Time</td>
                    </tr>
                    <tr className="subhead">
                        <td>satoshis<br/>per byte</td>
                        <td>transactions in mempool<br/>transactions since 24 hours</td>
                        <td>estimated<br/>in blocks</td>
                        <td>estimated<br/>in minutes</td>
                    </tr>
                    </thead>
                    <tbody>{
                        this.state.fees.length == 0
                            ? <tr><td colSpan="4"><h1>{this.state.message}</h1></td></tr>
                            : this.state.fees.map(renderFee.bind(this))
                    }</tbody>
                </table>

                <div className="faqs">

                    <h2>Which fee should I use?</h2>
                    { !this.state.medianTxSize ? <p>(Waiting for data...)</p> :
                        <p>
                            The fastest and cheapest transaction fee is currently <strong>{fastestFee.maxFee} satoshis/byte</strong>, shown in green at the top.
                            <br/>
                            For the median transaction size of <strong>{this.state.medianTxSize} bytes</strong>, this results in a fee of <strong>{txFeeCommas} satoshis</strong>.
                        </p>
                    }

                    <a name="fees" />
                    <h2>What are the fees shown here?</h2>
                    <p>The fees displayed here are Satoshis (0.00000001 BTC) per byte of transaction data. Miners usually
                        include transactions with the highest fee/byte first. <br/>
                        Wallets should base their fee calculations on this number,
                        depending on how fast the user needs confirmations.</p>

                    <a name="delay" />
                    <h2>What does the delay mean?</h2>
                    <p>
                        The delay shown here is the predicted number of blocks the transactions will take to confirm.
                        If a transactions are predicted to have a delay between 1-3 blocks,
                        there is a 90% chance that they will be confirmed within that range (around 10 to 30 minutes).
                        <br />
                        Transactions with higher fees will often have 0 delay, which means they will likely be confirmed
                        with the next block (usually around 5-15 minutes).
                    </p>

                    <h2>How is the delay predicted?</h2>
                    <p>
                        The predictions are based on blockchain data of the last 3 hours, as well as the current pool
                        of unconfirmed transactions (mempool).<br />
                        First, a likely future mempool and miner behavior is predicted using Monte Carlo simulation.
                        From the simulations, it can be seen how fast transactions with different fees are likely to be
                        included in the upcoming blocks. <br />
                        The predicted delay shown here is chosen to represent a 90% confidence interval.
                    </p>

                    <h2>Is there an API for developers?</h2>
                    <p>
                        Yes. <Link to="/api">See the API documentation here.</Link>
                    </p>

                </div>

                <div className="footer">
                    <h2>I would like to support this site.</h2>
                    If you like this service,
                    please support it by donating Bitcoin here:

                    <div style={{textAlign: 'center'}}>
                        <a href="bitcoin:1N1wUSBuLR3gEygK5HuZt1WoBxGFxgdAJ1?amount=0.01&label=cointape.com">
                            <img src="/assets/images/qr.png" />
                        </a>
                        <br />
                        <a href="bitcoin:1N1wUSBuLR3gEygK5HuZt1WoBxGFxgdAJ1?amount=0.01&label=cointape.com">
                            1N1wUSBuLR3gEygK5HuZt1WoBxGFxgdAJ1
                        </a>
                    </div>

                    <h2>I have some feedback for you!</h2>
                    <p>Sure, drop us a line any time.</p>
                </div>

                <Footer />

            </div>;
        }
    });


    var Api = React.createClass({
        render: function () {

            return <div id="overview">
                <Header />

                <div className="faqs">

                    <h1>CoinTape Developer API</h1>

                    <p>
                        Get current CoinTape predictions in JSON format for wallet or other services.<br/>
                        Current API Rate Limit: 5000 requests per hour.<br/>
                    </p>


                    <h2>Recommended Transaction Fees</h2>
                    <a href="http://cointape.21.co/api/v1/fees/recommended">http://cointape.21.co/api/v1<strong>/fees/recommended</strong></a>
                    <p>
                        Example response:
                        <pre>{'{ "fastestFee": 40, "halfHourFee": 20, "hourFee": 10 }'}</pre>
                        <strong>fastestFee</strong>: The lowest fee (in satoshis per byte) that will currently result in the fastest transaction confirmations (usually 0 to 1 block delay).<br />
                        <strong>halfHourFee</strong>: The lowest fee (in satoshis per byte) that will confirm transactions within half an hour (with 90% probability).<br />
                        <strong>hourFee</strong>: The lowest fee (in satoshis per byte) that will confirm transactions within an hour (with 90% probability).<br />
                    </p>

                    <h2>Transaction Fees Summary</h2>
                    <a href="http://cointape.21.co/api/v1/fees/list">http://cointape.21.co/api/v1<strong>/fees/list</strong></a>
                    <p>
                        Example response:
                        <pre>{'{ "fees": [ \r\n  {"minFee":0,"maxFee":0,"dayCount":545,"memCount":87,\r\n  "minDelay":4,"maxDelay":32,"minMinutes":20,"maxMinutes":420},\r\n...\r\n ] }'}</pre>
                        Returns a list of Fee objects that contain predictions about fees in the given range from <strong>minFee</strong> to <strong>maxFee</strong> in satoshis/byte.<br />
                        The <strong>Fee</strong> objects have the following properties (aside from the minFee-maxFee range they refer to):<br />
                        <strong>dayCount</strong>: Number of confirmed transactions with this fee in the last 24 hours.<br />
                        <strong>memCount</strong>: Number of unconfirmed transactions with this fee.<br />
                        <strong>minDelay</strong>: Estimated minimum delay (in blocks) until transaction is confirmed (90% confidence interval).<br />
                        <strong>maxDelay</strong>: Estimated maximum delay (in blocks) until transaction is confirmed (90% confidence interval).<br />
                        <strong>minMinutes</strong>: Estimated minimum time (in minutes) until transaction is confirmed (90% confidence interval).<br />
                        <strong>maxMinutes</strong>: Estimated maximum time (in minutes) until transaction is confirmed (90% confidence interval).<br />
                    </p>

                    <h2>Error codes</h2>
                    <p>
                        Status 503: Service unavailable (please wait while predictions are being generated) <br />
                        Status 429: Too many requests (API rate limit has been reached) <br />
                    </p>

                </div>

                <Footer />

            </div>;
        }
    });

    var routes = (
        <Route path="/" handler={BaseApp} >
            <Route path="/" handler={Index} />
            <Route path="/api" handler={Api} />
        </Route>
    );

    Router.run(routes, Router.HistoryLocation, function(Root) {
        React.render(<Root/>, document.getElementById("app-container"));
    });

});

var config = {
  mode: 'production',
  devtool: false,
  resolve: {
    modules: [
      "###EXECUTOR_DIR###/META-INF/node_modules"
    ]
  },
  plugins: [],
  module: {
    rules: [
        {test: /\.(woff|woff2)(\?v=\d+\.\d+\.\d+)?$/, loader: 'url-loader?limit=10000&mimetype=application/font-woff'},
        {test: /\.ttf(\?v=\d+\.\d+\.\d+)?$/, loader: 'url-loader?limit=10000&mimetype=application/octet-stream'},
        {test: /\.eot(\?v=\d+\.\d+\.\d+)?$/, loader: 'file-loader'},
        {test: /\.svg(\?v=\d+\.\d+\.\d+)?$/, loader: 'url-loader?limit=10000&mimetype=image/svg+xml'},
        {test: /\.css$/, loader: "style-loader!css-loader" },
        {test: /\.(jpe?g|png|gif)$/i, loader: 'file-loader', options: { esModule: false } }
    ]
  },
  entry: [
    "###TEMP_DIR###/main.js"
  ],
  output: {
    path: "###TEMP_DIR###",
    filename: "main.bundle.js"
  }
};

;(function() {
    const UglifyJSPlugin = require('uglifyjs-webpack-plugin');

    config.optimization = {
        minimizer: [
             new UglifyJSPlugin({
                uglifyOptions: {
                    compress: {
                        unused: false
                    }
                }
             })
        ]
    }
})();

;(function() {
    const webpack = require('webpack')

    config.plugins.push(new webpack.ProvidePlugin({
	$: "jquery",
	jQuery: "jquery",
	"window.jQuery": "jquery"
    }));
})();

module.exports = config

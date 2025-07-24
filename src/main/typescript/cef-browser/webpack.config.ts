const path = require('path');

module.exports = (env : any, argv : any) => [{
    entry: './src/main.ts',
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                use: 'ts-loader',
                exclude: /node_modules/,
            },
        ],
    },
    resolve: {
        extensions: [ '.tsx', '.ts', '.js' ],
    },
    devtool: 'inline-source-map',
    mode: "development",
    output: {
        filename: 'bundle.js',
        path: path.resolve(__dirname, '../../resources/js')
    }
}];
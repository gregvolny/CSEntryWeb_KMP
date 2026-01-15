
import * as _ref_Li9jc2VudHJ5S01QLmpz_ from './csentryKMP.js';
import * as _ref_QGpzLWpvZGEvY29yZQ_ from '@js-joda/core';
import { instantiate } from './csentry-web.uninstantiated.mjs';

const exports = (await instantiate({
    './csentryKMP.js': _ref_Li9jc2VudHJ5S01QLmpz_,
    '@js-joda/core': _ref_QGpzLWpvZGEvY29yZQ_
})).exports;

export default new Proxy(exports, {
    _shownError: false,
    get(target, prop) {
        if (!this._shownError) {
            this._shownError = true;
            throw new Error("Do not use default import. Use the corresponding named import instead.")
        }
    }
});
export const {
    _initialize,
    memory
} = exports;


/******/ (() => { // webpackBootstrap
/******/ 	"use strict";
/******/ 	var __webpack_modules__ = ({

/***/ "./node_modules/@itihon/position-observer/dist/esm/index.js":
/*!******************************************************************!*\
  !*** ./node_modules/@itihon/position-observer/dist/esm/index.js ***!
  \******************************************************************/
/***/ ((__webpack_module__, __webpack_exports__, __webpack_require__) => {

__webpack_require__.a(__webpack_module__, async (__webpack_handle_async_dependencies__, __webpack_async_result__) => { try {
__webpack_require__.r(__webpack_exports__);
/* harmony export */ __webpack_require__.d(__webpack_exports__, {
/* harmony export */   "default": () => (/* binding */ PositionObserver)
/* harmony export */ });
/* harmony import */ var request_animation_frame_loop__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! request-animation-frame-loop */ "./node_modules/request-animation-frame-loop/dist/esm/index.js");


/**
 * A wrapper for intersection observer
 * needed to have access to more than one context inside a callback
 */

/**
 * @callback IntersectionObserverHFCallback
 * @param {Array<IntersectionObserverEntry>} entries
 * @returns {void}
 */
class IntersectionObserverHF extends IntersectionObserver {
  /**
   * @param {IntersectionObserverHFCallback} callback
   * @param {IntersectionObserverInit} options
   * @param {*} ctx
   */
  constructor(callback, options, ctx) {
    super(callback, options);
    this.ctx = ctx;
  }
}

// initial viewport rect
const viewportRect = await new Promise((res) => {
  const observer = new IntersectionObserver(
    (entries) => {
      res(entries[0].rootBounds);
      observer.unobserve(document.documentElement);
    },
    { root: document },
  );
  observer.observe(document.documentElement);
});

const { width: viewportWidth, height: viewportHeight } = viewportRect;

/**
 * @callback PositionObserverCallback
 * @param {HTMLElement} target
 * @param {DOMRect} targetRect
 * @param {*} ctx
 * @returns {void}
 */

/**
 * @typedef RAFLContext
 * @property {HTMLElement} target
 * @property {FourSideObserver} observers
 * @property {DOMRect} rect
 * @property {PositionObserver} self
 * @property {typeof PositionObserver} staticSelf
 * @property {Set<"top" | "right" | "bottom" | "left">} recreationList
 */

/**
 * @callback RAFLCallback
 * @param {RAFLContext} ctx
 * @param {RequestAnimationFrameLoop} loop
 * @param {number} timestamp
 * @returns {void}
 */

/**
 * @typedef FourSideObserver
 * @property {IntersectionObserverHF} top
 * @property {IntersectionObserverHF} right
 * @property {IntersectionObserverHF} bottom
 * @property {IntersectionObserverHF} left
 */

class PositionObserver {
  /* dependencies */
  static #IntersectionObserverHF = IntersectionObserverHF;
  static #RequestAnimationFrameLoop = request_animation_frame_loop__WEBPACK_IMPORTED_MODULE_0__["default"];

  static #vw = viewportWidth;
  static #vh = viewportHeight;

  /** @type {IntersectionObserverInit} */
  static #options = {
    rootMargin: undefined,
    threshold: Array.from({ length: 101 }, (_, idx) => idx * 0.01),
    root: document,
  };

  /**
   * @type {RAFLCallback}
   */
  static #unobserve(ctx) {
    const { observers, target } = ctx;

    observers.top.unobserve(target);
    observers.right.unobserve(target);
    observers.bottom.unobserve(target);
    observers.left.unobserve(target);
  }

  /**
   * @type {RAFLCallback}
   */
  static #positionChanging(ctx, loop) {
    const { target, rect, self } = ctx;

    const targetRect = target.getBoundingClientRect();
    const { left, top, right, bottom } = targetRect;

    if (
      left === rect.left &&
      top === rect.top &&
      right === rect.right &&
      bottom === rect.bottom
    ) {
      loop.stop();
    } else {
      rect.left = left;
      rect.top = top;
      rect.right = right;
      rect.bottom = bottom;

      self.#callback(target, targetRect, self.#ctx);
    }
  }

  /**
   * @type {RAFLCallback}
   */
  static #createObserver(ctx) {
    const { target, rect, observers, self, staticSelf, recreationList } = ctx;
    const cb = self.#observerCallback;
    const { top, right, bottom, left } = rect;

    const IntersectionObserverHF = staticSelf.#IntersectionObserverHF;
    const options = staticSelf.#options;

    if (recreationList.has('top')) {
      // recreate the top observer
      options.rootMargin = `0px 0px ${-(staticSelf.#vh - top - 2)}px 0px`;
      if (observers.top) observers.top.unobserve(target);
      observers.top = new IntersectionObserverHF(cb, options, self);
    }

    if (recreationList.has('right')) {
      // recreate the right observer
      options.rootMargin = `0px 0px 0px ${-(right - 2)}px`;
      if (observers.right) observers.right.unobserve(target);
      observers.right = new IntersectionObserverHF(cb, options, self);
    }

    if (recreationList.has('bottom')) {
      // recreate the bottom observer
      options.rootMargin = `${-(bottom - 2)}px 0px 0px 0px`;
      if (observers.bottom) observers.bottom.unobserve(target);
      observers.bottom = new IntersectionObserverHF(cb, options, self);
    }

    if (recreationList.has('left')) {
      // recreate the left observer
      options.rootMargin = `0px ${-(staticSelf.#vw - left - 2)}px 0px 0px`;
      if (observers.left) observers.left.unobserve(target);
      observers.left = new IntersectionObserverHF(cb, options, self);
    }

    recreationList.clear();
    observers.top.observe(target);
    observers.right.observe(target);
    observers.bottom.observe(target);
    observers.left.observe(target);
  }

  #callback;
  #ctx;

  /** @type {Map<HTMLElement, FourSideObserver>} */
  #observers = new Map();

  /** @type {Map<HTMLElement, RequestAnimationFrameLoop>} */
  #rafLoops = new Map();

  /** @type {Map<HTMLElement, RAFLContext>} */
  #rafCtxs = new Map();

  /**
   * @param {Array<IntersectionObserverEntry>} entries
   */
  #observerCallback(entries) {
    // the "this" here is an instance of IntersectionObserverHF

    /**
     * @type {PositionObserver}
     */
    const self = this.ctx;
    const { target, boundingClientRect } = entries[entries.length - 1];

    const {
      top: targetTop,
      right: targetRight,
      bottom: targetBottom,
      left: targetLeft,
    } = boundingClientRect;

    const observers = self.#observers.get(target);
    const recreationList = self.#rafCtxs.get(target).recreationList;

    const topRecords =
      this === observers.top ? entries : observers.top.takeRecords();
    const rightRecords =
      this === observers.right ? entries : observers.right.takeRecords();
    const bottomRecords =
      this === observers.bottom ? entries : observers.bottom.takeRecords();
    const leftRecords =
      this === observers.left ? entries : observers.left.takeRecords();

    // display: none; protection against an infinite loop
    if (!target.offsetParent) {
      self.#callback(target, boundingClientRect, self.#ctx);
      return;
    }

    if (topRecords.length) {
      const { bottom: rootBottom, width: rootWidth } =
        topRecords[topRecords.length - 1].rootBounds;

      PositionObserver.#vw = rootWidth;

      const intersectionHeight = rootBottom - targetTop;

      if (!(1 < intersectionHeight && intersectionHeight < 3)) {
        if (targetTop >= 0) {
          recreationList.add('top');
        }
      }
    }

    if (rightRecords.length) {
      const {
        right: rootRight,
        left: rootLeft,
        height: rootHeight,
      } = rightRecords[rightRecords.length - 1].rootBounds;

      PositionObserver.#vh = rootHeight;
      PositionObserver.#vw = rootRight;

      const intersectionWidth = targetRight - rootLeft;

      if (!(1 < intersectionWidth && intersectionWidth < 3)) {
        if (targetRight <= rootRight) {
          recreationList.add('right');
        }
      }
    }

    if (bottomRecords.length) {
      const {
        top: rootTop,
        bottom: rootBottom,
        width: rootWidth,
      } = bottomRecords[bottomRecords.length - 1].rootBounds;

      PositionObserver.#vw = rootWidth;
      PositionObserver.#vh = rootBottom;

      const intersectionHeight = targetBottom - rootTop;

      if (!(1 < intersectionHeight && intersectionHeight < 3)) {
        if (targetBottom <= rootBottom) {
          recreationList.add('bottom');
        }
      }
    }

    if (leftRecords.length) {
      const { right: rootRight, height: rootHeight } =
        leftRecords[leftRecords.length - 1].rootBounds;

      PositionObserver.#vh = rootHeight;

      const intersectionWidth = rootRight - targetLeft;

      if (!(1 < intersectionWidth && intersectionWidth < 3)) {
        if (targetLeft >= 0) {
          recreationList.add('left');
        }
      }
    }

    if (recreationList.size) {
      self.#callback(target, boundingClientRect, self.#ctx);
      self.#rafLoops.get(target).start();
    }
  }

  /**
   * @param {PositionObserverCallback} callback
   * @param {*} ctx
   */
  constructor(callback, ctx) {
    this.#callback = callback;
    this.#ctx = ctx;
  }

  /**
   * @param {HTMLElement} target
   */
  observe(target) {
    if (this.#observers.has(target)) return;

    const observers = { top: null, right: null, bottom: null, left: null };
    const targetRect = target.getBoundingClientRect();

    /**
     * @type {RAFLContext}
     */
    const ctx = {
      target,
      observers,
      rect: {
        top: targetRect.top,
        right: targetRect.right,
        bottom: targetRect.bottom,
        left: targetRect.left,
      },
      self: this,
      staticSelf: PositionObserver,
      recreationList: new Set(['top', 'right', 'bottom', 'left']),
    };

    const rafLoop = new PositionObserver.#RequestAnimationFrameLoop(ctx)
      .started(PositionObserver.#unobserve)
      .each(PositionObserver.#positionChanging)
      .stopped(PositionObserver.#createObserver);

    PositionObserver.#createObserver(ctx);

    this.#observers.set(target, observers);
    this.#rafLoops.set(target, rafLoop);
    this.#rafCtxs.set(target, ctx);

    this.#callback(target, targetRect, this.#ctx); // initial callback invokation
  }

  /**
   * @param {HTMLElement} target
   */
  unobserve(target) {
    if (!this.#observers.has(target)) return;

    const observers = this.#observers.get(target);
    const ctx = { target, observers };

    PositionObserver.#unobserve(ctx);

    this.#observers.delete(target);
    this.#rafLoops.delete(target);
    this.#rafCtxs.delete(target);
  }

  disconnect() {
    this.#observers.forEach((_, target) => this.unobserve(target));
  }

  /**
   * @returns {MapIterator<HTMLElement>}
   */
  getTargets() {
    return this.#observers.keys();
  }
}



__webpack_async_result__();
} catch(e) { __webpack_async_result__(e); } }, 1);

/***/ }),

/***/ "./node_modules/request-animation-frame-loop/dist/esm/index.js":
/*!*********************************************************************!*\
  !*** ./node_modules/request-animation-frame-loop/dist/esm/index.js ***!
  \*********************************************************************/
/***/ ((__unused_webpack_module, __webpack_exports__, __webpack_require__) => {

__webpack_require__.r(__webpack_exports__);
/* harmony export */ __webpack_require__.d(__webpack_exports__, {
/* harmony export */   "default": () => (/* binding */ RequestAnimationFrameLoop)
/* harmony export */ });
/**
 * @callback RequestAnimationFrameStateCallback
 * @param {*} ctx
 * @param {RequestAnimationFrameLoop} loop
 * @param {number} timestamp
 */

class RequestAnimationFrameLoop {
  #loopID;
  #ctx;
  #startedCB = Function.prototype;
  #eachCB = Function.prototype;
  #stoppedCB = Function.prototype;

  #rAFCallback = (timestamp) => {
    this.#loopID = requestAnimationFrame(this.#rAFCallback);
    this.#eachCB(this.#ctx, this, timestamp);
  };

  constructor(ctx) {
    this.#ctx = ctx;
  }

  /**
   * @param {RequestAnimationFrameStateCallback} cb
   * @returns {RequestAnimationFrameLoop}
   */
  started(cb) {
    this.#startedCB = cb;
    return this;
  }

  /**
   * @param {RequestAnimationFrameStateCallback} cb
   * @returns {RequestAnimationFrameLoop}
   */
  each(cb) {
    this.#eachCB = cb;
    return this;
  }

  /**
   * @param {RequestAnimationFrameStateCallback} cb
   * @returns {RequestAnimationFrameLoop}
   */
  stopped(cb) {
    this.#stoppedCB = cb;
    return this;
  }

  start() {
    if (this.#loopID === undefined) {
      this.#loopID = requestAnimationFrame(this.#rAFCallback);
      this.#startedCB(this.#ctx, this, 0);
    }
  }

  stop() {
    cancelAnimationFrame(this.#loopID);
    this.#stoppedCB(this.#ctx, this, 0);
    this.#loopID = undefined;
  }
}




/***/ }),

/***/ "./src/main.ts":
/*!*********************!*\
  !*** ./src/main.ts ***!
  \*********************/
/***/ ((module, __webpack_exports__, __webpack_require__) => {

__webpack_require__.a(module, async (__webpack_handle_async_dependencies__, __webpack_async_result__) => { try {
__webpack_require__.r(__webpack_exports__);
/* harmony import */ var _itihon_position_observer__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! @itihon/position-observer */ "./node_modules/@itihon/position-observer/dist/esm/index.js");
var __webpack_async_dependencies__ = __webpack_handle_async_dependencies__([_itihon_position_observer__WEBPACK_IMPORTED_MODULE_0__]);
_itihon_position_observer__WEBPACK_IMPORTED_MODULE_0__ = (__webpack_async_dependencies__.then ? (await __webpack_async_dependencies__)() : __webpack_async_dependencies__)[0];

let nextDomId = 1;
let unreportedChanges = new Map();
let reportCallback = null;
function eq(rect, other) {
    return rect.x == other.x && rect.y == other.y && rect.width == other.width && rect.height == other.height;
}
function reportIfPossible() {
    if (reportCallback != null && unreportedChanges.size > 0) {
        reportCallback(Array.from(unreportedChanges.values()));
        unreportedChanges.clear();
        reportCallback = null;
    }
}
function reportChanged(target, targetId, newRect) {
    const serialized = newRect ? ({
        x: newRect.x,
        y: newRect.y,
        width: newRect.width,
        height: newRect.height,
        captureScroll: target.classList.contains("browserScrollCapture")
    }) : null;
    const oldSerialized = target.lastBrowserRect;
    if (oldSerialized === undefined || ((serialized === null) !== (oldSerialized === null)) || (serialized != null && oldSerialized !== null && !eq(serialized, oldSerialized))) {
        unreportedChanges.set(targetId, ({
            targetId,
            newRect: serialized,
        }));
        target.lastBrowserRect = serialized;
        reportIfPossible();
    }
}
function getDomId(element) {
    const id = element.browserDomId;
    if (id) {
        return id;
    }
    else {
        return null;
    }
}
function ensureDomId(element) {
    const existing = getDomId(element);
    if (existing) {
        return existing;
    }
    nextDomId += 1;
    element.browserDomId = nextDomId;
    return nextDomId;
}
function onBoundsChanged(target, targetRect, ctx) {
    if (target instanceof HTMLElement) {
        const domId = getDomId(target);
        if (domId != null) {
            reportChanged(target, domId, targetRect);
        }
    }
}
const positionObserver = new _itihon_position_observer__WEBPACK_IMPORTED_MODULE_0__["default"](onBoundsChanged, {});
function startObserving(childNode) {
    const domId = ensureDomId(childNode);
    positionObserver.observe(childNode);
    reportChanged(childNode, domId, childNode.getBoundingClientRect());
}
const observer = new MutationObserver(mutations => {
    for (const mutation of mutations) {
        if (mutation.type == "childList") {
            for (const childNode of mutation.addedNodes) {
                if (childNode instanceof HTMLElement) {
                    if (childNode.classList.contains("browserClickCapture")) {
                        startObserving(childNode);
                    }
                }
            }
            for (const childNode of mutation.removedNodes) {
                if (childNode instanceof HTMLElement) {
                    const domId = getDomId(childNode);
                    if (domId != null) {
                        positionObserver.unobserve(childNode);
                        reportChanged(childNode, domId, null);
                    }
                }
            }
        }
    }
});
for (const existing of document.querySelectorAll(".browserClickCapture")) {
    if (existing instanceof HTMLElement) {
        startObserving(existing);
    }
}
observer.observe(document.body, { childList: true, subtree: true });
window.registerReportCallback = function (cb) {
    reportCallback = cb;
    reportIfPossible();
};

__webpack_async_result__();
} catch(e) { __webpack_async_result__(e); } });

/***/ })

/******/ 	});
/************************************************************************/
/******/ 	// The module cache
/******/ 	var __webpack_module_cache__ = {};
/******/ 	
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/ 		// Check if module is in cache
/******/ 		var cachedModule = __webpack_module_cache__[moduleId];
/******/ 		if (cachedModule !== undefined) {
/******/ 			return cachedModule.exports;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = __webpack_module_cache__[moduleId] = {
/******/ 			// no module.id needed
/******/ 			// no module.loaded needed
/******/ 			exports: {}
/******/ 		};
/******/ 	
/******/ 		// Execute the module function
/******/ 		__webpack_modules__[moduleId](module, module.exports, __webpack_require__);
/******/ 	
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/ 	
/************************************************************************/
/******/ 	/* webpack/runtime/async module */
/******/ 	(() => {
/******/ 		var webpackQueues = typeof Symbol === "function" ? Symbol("webpack queues") : "__webpack_queues__";
/******/ 		var webpackExports = typeof Symbol === "function" ? Symbol("webpack exports") : "__webpack_exports__";
/******/ 		var webpackError = typeof Symbol === "function" ? Symbol("webpack error") : "__webpack_error__";
/******/ 		var resolveQueue = (queue) => {
/******/ 			if(queue && queue.d < 1) {
/******/ 				queue.d = 1;
/******/ 				queue.forEach((fn) => (fn.r--));
/******/ 				queue.forEach((fn) => (fn.r-- ? fn.r++ : fn()));
/******/ 			}
/******/ 		}
/******/ 		var wrapDeps = (deps) => (deps.map((dep) => {
/******/ 			if(dep !== null && typeof dep === "object") {
/******/ 				if(dep[webpackQueues]) return dep;
/******/ 				if(dep.then) {
/******/ 					var queue = [];
/******/ 					queue.d = 0;
/******/ 					dep.then((r) => {
/******/ 						obj[webpackExports] = r;
/******/ 						resolveQueue(queue);
/******/ 					}, (e) => {
/******/ 						obj[webpackError] = e;
/******/ 						resolveQueue(queue);
/******/ 					});
/******/ 					var obj = {};
/******/ 					obj[webpackQueues] = (fn) => (fn(queue));
/******/ 					return obj;
/******/ 				}
/******/ 			}
/******/ 			var ret = {};
/******/ 			ret[webpackQueues] = x => {};
/******/ 			ret[webpackExports] = dep;
/******/ 			return ret;
/******/ 		}));
/******/ 		__webpack_require__.a = (module, body, hasAwait) => {
/******/ 			var queue;
/******/ 			hasAwait && ((queue = []).d = -1);
/******/ 			var depQueues = new Set();
/******/ 			var exports = module.exports;
/******/ 			var currentDeps;
/******/ 			var outerResolve;
/******/ 			var reject;
/******/ 			var promise = new Promise((resolve, rej) => {
/******/ 				reject = rej;
/******/ 				outerResolve = resolve;
/******/ 			});
/******/ 			promise[webpackExports] = exports;
/******/ 			promise[webpackQueues] = (fn) => (queue && fn(queue), depQueues.forEach(fn), promise["catch"](x => {}));
/******/ 			module.exports = promise;
/******/ 			body((deps) => {
/******/ 				currentDeps = wrapDeps(deps);
/******/ 				var fn;
/******/ 				var getResult = () => (currentDeps.map((d) => {
/******/ 					if(d[webpackError]) throw d[webpackError];
/******/ 					return d[webpackExports];
/******/ 				}))
/******/ 				var promise = new Promise((resolve) => {
/******/ 					fn = () => (resolve(getResult));
/******/ 					fn.r = 0;
/******/ 					var fnQueue = (q) => (q !== queue && !depQueues.has(q) && (depQueues.add(q), q && !q.d && (fn.r++, q.push(fn))));
/******/ 					currentDeps.map((dep) => (dep[webpackQueues](fnQueue)));
/******/ 				});
/******/ 				return fn.r ? promise : getResult();
/******/ 			}, (err) => ((err ? reject(promise[webpackError] = err) : outerResolve(exports)), resolveQueue(queue)));
/******/ 			queue && queue.d < 0 && (queue.d = 0);
/******/ 		};
/******/ 	})();
/******/ 	
/******/ 	/* webpack/runtime/define property getters */
/******/ 	(() => {
/******/ 		// define getter functions for harmony exports
/******/ 		__webpack_require__.d = (exports, definition) => {
/******/ 			for(var key in definition) {
/******/ 				if(__webpack_require__.o(definition, key) && !__webpack_require__.o(exports, key)) {
/******/ 					Object.defineProperty(exports, key, { enumerable: true, get: definition[key] });
/******/ 				}
/******/ 			}
/******/ 		};
/******/ 	})();
/******/ 	
/******/ 	/* webpack/runtime/hasOwnProperty shorthand */
/******/ 	(() => {
/******/ 		__webpack_require__.o = (obj, prop) => (Object.prototype.hasOwnProperty.call(obj, prop))
/******/ 	})();
/******/ 	
/******/ 	/* webpack/runtime/make namespace object */
/******/ 	(() => {
/******/ 		// define __esModule on exports
/******/ 		__webpack_require__.r = (exports) => {
/******/ 			if(typeof Symbol !== 'undefined' && Symbol.toStringTag) {
/******/ 				Object.defineProperty(exports, Symbol.toStringTag, { value: 'Module' });
/******/ 			}
/******/ 			Object.defineProperty(exports, '__esModule', { value: true });
/******/ 		};
/******/ 	})();
/******/ 	
/************************************************************************/
/******/ 	
/******/ 	// startup
/******/ 	// Load entry module and return exports
/******/ 	// This entry module used 'module' so it can't be inlined
/******/ 	var __webpack_exports__ = __webpack_require__("./src/main.ts");
/******/ 	
/******/ })()
;
//# sourceMappingURL=data:application/json;charset=utf-8;base64,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O1dBQ0E7V0FDQTtXQUNBO1dBQ0E7V0FDQTtXQUNBO1dBQ0E7V0FDQSxFQUFFO1dBQ0Y7V0FDQSxzR0FBc0c7V0FDdEc7V0FDQTtXQUNBO1dBQ0E7V0FDQTtXQUNBO1dBQ0E7V0FDQSxHQUFHO1dBQ0g7V0FDQTtXQUNBO1dBQ0E7V0FDQTtXQUNBLEdBQUc7V0FDSDtXQUNBLEVBQUU7V0FDRjtXQUNBOzs7OztXQ2hFQTtXQUNBO1dBQ0E7V0FDQTtXQUNBLHlDQUF5Qyx3Q0FBd0M7V0FDakY7V0FDQTtXQUNBOzs7OztXQ1BBOzs7OztXQ0FBO1dBQ0E7V0FDQTtXQUNBLHVEQUF1RCxpQkFBaUI7V0FDeEU7V0FDQSxnREFBZ0QsYUFBYTtXQUM3RDs7Ozs7VUVOQTtVQUNBO1VBQ0E7VUFDQSIsInNvdXJjZXMiOlsid2VicGFjazovL2ZlcmFsLy4vbm9kZV9tb2R1bGVzL0BpdGlob24vcG9zaXRpb24tb2JzZXJ2ZXIvZGlzdC9lc20vaW5kZXguanMiLCJ3ZWJwYWNrOi8vZmVyYWwvLi9ub2RlX21vZHVsZXMvcmVxdWVzdC1hbmltYXRpb24tZnJhbWUtbG9vcC9kaXN0L2VzbS9pbmRleC5qcyIsIndlYnBhY2s6Ly9mZXJhbC8uL3NyYy9tYWluLnRzIiwid2VicGFjazovL2ZlcmFsL3dlYnBhY2svYm9vdHN0cmFwIiwid2VicGFjazovL2ZlcmFsL3dlYnBhY2svcnVudGltZS9hc3luYyBtb2R1bGUiLCJ3ZWJwYWNrOi8vZmVyYWwvd2VicGFjay9ydW50aW1lL2RlZmluZSBwcm9wZXJ0eSBnZXR0ZXJzIiwid2VicGFjazovL2ZlcmFsL3dlYnBhY2svcnVudGltZS9oYXNPd25Qcm9wZXJ0eSBzaG9ydGhhbmQiLCJ3ZWJwYWNrOi8vZmVyYWwvd2VicGFjay9ydW50aW1lL21ha2UgbmFtZXNwYWNlIG9iamVjdCIsIndlYnBhY2s6Ly9mZXJhbC93ZWJwYWNrL2JlZm9yZS1zdGFydHVwIiwid2VicGFjazovL2ZlcmFsL3dlYnBhY2svc3RhcnR1cCIsIndlYnBhY2s6Ly9mZXJhbC93ZWJwYWNrL2FmdGVyLXN0YXJ0dXAiXSwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IFJlcXVlc3RBbmltYXRpb25GcmFtZUxvb3AgZnJvbSAncmVxdWVzdC1hbmltYXRpb24tZnJhbWUtbG9vcCc7XG5cbi8qKlxuICogQSB3cmFwcGVyIGZvciBpbnRlcnNlY3Rpb24gb2JzZXJ2ZXJcbiAqIG5lZWRlZCB0byBoYXZlIGFjY2VzcyB0byBtb3JlIHRoYW4gb25lIGNvbnRleHQgaW5zaWRlIGEgY2FsbGJhY2tcbiAqL1xuXG4vKipcbiAqIEBjYWxsYmFjayBJbnRlcnNlY3Rpb25PYnNlcnZlckhGQ2FsbGJhY2tcbiAqIEBwYXJhbSB7QXJyYXk8SW50ZXJzZWN0aW9uT2JzZXJ2ZXJFbnRyeT59IGVudHJpZXNcbiAqIEByZXR1cm5zIHt2b2lkfVxuICovXG5jbGFzcyBJbnRlcnNlY3Rpb25PYnNlcnZlckhGIGV4dGVuZHMgSW50ZXJzZWN0aW9uT2JzZXJ2ZXIge1xuICAvKipcbiAgICogQHBhcmFtIHtJbnRlcnNlY3Rpb25PYnNlcnZlckhGQ2FsbGJhY2t9IGNhbGxiYWNrXG4gICAqIEBwYXJhbSB7SW50ZXJzZWN0aW9uT2JzZXJ2ZXJJbml0fSBvcHRpb25zXG4gICAqIEBwYXJhbSB7Kn0gY3R4XG4gICAqL1xuICBjb25zdHJ1Y3RvcihjYWxsYmFjaywgb3B0aW9ucywgY3R4KSB7XG4gICAgc3VwZXIoY2FsbGJhY2ssIG9wdGlvbnMpO1xuICAgIHRoaXMuY3R4ID0gY3R4O1xuICB9XG59XG5cbi8vIGluaXRpYWwgdmlld3BvcnQgcmVjdFxuY29uc3Qgdmlld3BvcnRSZWN0ID0gYXdhaXQgbmV3IFByb21pc2UoKHJlcykgPT4ge1xuICBjb25zdCBvYnNlcnZlciA9IG5ldyBJbnRlcnNlY3Rpb25PYnNlcnZlcihcbiAgICAoZW50cmllcykgPT4ge1xuICAgICAgcmVzKGVudHJpZXNbMF0ucm9vdEJvdW5kcyk7XG4gICAgICBvYnNlcnZlci51bm9ic2VydmUoZG9jdW1lbnQuZG9jdW1lbnRFbGVtZW50KTtcbiAgICB9LFxuICAgIHsgcm9vdDogZG9jdW1lbnQgfSxcbiAgKTtcbiAgb2JzZXJ2ZXIub2JzZXJ2ZShkb2N1bWVudC5kb2N1bWVudEVsZW1lbnQpO1xufSk7XG5cbmNvbnN0IHsgd2lkdGg6IHZpZXdwb3J0V2lkdGgsIGhlaWdodDogdmlld3BvcnRIZWlnaHQgfSA9IHZpZXdwb3J0UmVjdDtcblxuLyoqXG4gKiBAY2FsbGJhY2sgUG9zaXRpb25PYnNlcnZlckNhbGxiYWNrXG4gKiBAcGFyYW0ge0hUTUxFbGVtZW50fSB0YXJnZXRcbiAqIEBwYXJhbSB7RE9NUmVjdH0gdGFyZ2V0UmVjdFxuICogQHBhcmFtIHsqfSBjdHhcbiAqIEByZXR1cm5zIHt2b2lkfVxuICovXG5cbi8qKlxuICogQHR5cGVkZWYgUkFGTENvbnRleHRcbiAqIEBwcm9wZXJ0eSB7SFRNTEVsZW1lbnR9IHRhcmdldFxuICogQHByb3BlcnR5IHtGb3VyU2lkZU9ic2VydmVyfSBvYnNlcnZlcnNcbiAqIEBwcm9wZXJ0eSB7RE9NUmVjdH0gcmVjdFxuICogQHByb3BlcnR5IHtQb3NpdGlvbk9ic2VydmVyfSBzZWxmXG4gKiBAcHJvcGVydHkge3R5cGVvZiBQb3NpdGlvbk9ic2VydmVyfSBzdGF0aWNTZWxmXG4gKiBAcHJvcGVydHkge1NldDxcInRvcFwiIHwgXCJyaWdodFwiIHwgXCJib3R0b21cIiB8IFwibGVmdFwiPn0gcmVjcmVhdGlvbkxpc3RcbiAqL1xuXG4vKipcbiAqIEBjYWxsYmFjayBSQUZMQ2FsbGJhY2tcbiAqIEBwYXJhbSB7UkFGTENvbnRleHR9IGN0eFxuICogQHBhcmFtIHtSZXF1ZXN0QW5pbWF0aW9uRnJhbWVMb29wfSBsb29wXG4gKiBAcGFyYW0ge251bWJlcn0gdGltZXN0YW1wXG4gKiBAcmV0dXJucyB7dm9pZH1cbiAqL1xuXG4vKipcbiAqIEB0eXBlZGVmIEZvdXJTaWRlT2JzZXJ2ZXJcbiAqIEBwcm9wZXJ0eSB7SW50ZXJzZWN0aW9uT2JzZXJ2ZXJIRn0gdG9wXG4gKiBAcHJvcGVydHkge0ludGVyc2VjdGlvbk9ic2VydmVySEZ9IHJpZ2h0XG4gKiBAcHJvcGVydHkge0ludGVyc2VjdGlvbk9ic2VydmVySEZ9IGJvdHRvbVxuICogQHByb3BlcnR5IHtJbnRlcnNlY3Rpb25PYnNlcnZlckhGfSBsZWZ0XG4gKi9cblxuY2xhc3MgUG9zaXRpb25PYnNlcnZlciB7XG4gIC8qIGRlcGVuZGVuY2llcyAqL1xuICBzdGF0aWMgI0ludGVyc2VjdGlvbk9ic2VydmVySEYgPSBJbnRlcnNlY3Rpb25PYnNlcnZlckhGO1xuICBzdGF0aWMgI1JlcXVlc3RBbmltYXRpb25GcmFtZUxvb3AgPSBSZXF1ZXN0QW5pbWF0aW9uRnJhbWVMb29wO1xuXG4gIHN0YXRpYyAjdncgPSB2aWV3cG9ydFdpZHRoO1xuICBzdGF0aWMgI3ZoID0gdmlld3BvcnRIZWlnaHQ7XG5cbiAgLyoqIEB0eXBlIHtJbnRlcnNlY3Rpb25PYnNlcnZlckluaXR9ICovXG4gIHN0YXRpYyAjb3B0aW9ucyA9IHtcbiAgICByb290TWFyZ2luOiB1bmRlZmluZWQsXG4gICAgdGhyZXNob2xkOiBBcnJheS5mcm9tKHsgbGVuZ3RoOiAxMDEgfSwgKF8sIGlkeCkgPT4gaWR4ICogMC4wMSksXG4gICAgcm9vdDogZG9jdW1lbnQsXG4gIH07XG5cbiAgLyoqXG4gICAqIEB0eXBlIHtSQUZMQ2FsbGJhY2t9XG4gICAqL1xuICBzdGF0aWMgI3Vub2JzZXJ2ZShjdHgpIHtcbiAgICBjb25zdCB7IG9ic2VydmVycywgdGFyZ2V0IH0gPSBjdHg7XG5cbiAgICBvYnNlcnZlcnMudG9wLnVub2JzZXJ2ZSh0YXJnZXQpO1xuICAgIG9ic2VydmVycy5yaWdodC51bm9ic2VydmUodGFyZ2V0KTtcbiAgICBvYnNlcnZlcnMuYm90dG9tLnVub2JzZXJ2ZSh0YXJnZXQpO1xuICAgIG9ic2VydmVycy5sZWZ0LnVub2JzZXJ2ZSh0YXJnZXQpO1xuICB9XG5cbiAgLyoqXG4gICAqIEB0eXBlIHtSQUZMQ2FsbGJhY2t9XG4gICAqL1xuICBzdGF0aWMgI3Bvc2l0aW9uQ2hhbmdpbmcoY3R4LCBsb29wKSB7XG4gICAgY29uc3QgeyB0YXJnZXQsIHJlY3QsIHNlbGYgfSA9IGN0eDtcblxuICAgIGNvbnN0IHRhcmdldFJlY3QgPSB0YXJnZXQuZ2V0Qm91bmRpbmdDbGllbnRSZWN0KCk7XG4gICAgY29uc3QgeyBsZWZ0LCB0b3AsIHJpZ2h0LCBib3R0b20gfSA9IHRhcmdldFJlY3Q7XG5cbiAgICBpZiAoXG4gICAgICBsZWZ0ID09PSByZWN0LmxlZnQgJiZcbiAgICAgIHRvcCA9PT0gcmVjdC50b3AgJiZcbiAgICAgIHJpZ2h0ID09PSByZWN0LnJpZ2h0ICYmXG4gICAgICBib3R0b20gPT09IHJlY3QuYm90dG9tXG4gICAgKSB7XG4gICAgICBsb29wLnN0b3AoKTtcbiAgICB9IGVsc2Uge1xuICAgICAgcmVjdC5sZWZ0ID0gbGVmdDtcbiAgICAgIHJlY3QudG9wID0gdG9wO1xuICAgICAgcmVjdC5yaWdodCA9IHJpZ2h0O1xuICAgICAgcmVjdC5ib3R0b20gPSBib3R0b207XG5cbiAgICAgIHNlbGYuI2NhbGxiYWNrKHRhcmdldCwgdGFyZ2V0UmVjdCwgc2VsZi4jY3R4KTtcbiAgICB9XG4gIH1cblxuICAvKipcbiAgICogQHR5cGUge1JBRkxDYWxsYmFja31cbiAgICovXG4gIHN0YXRpYyAjY3JlYXRlT2JzZXJ2ZXIoY3R4KSB7XG4gICAgY29uc3QgeyB0YXJnZXQsIHJlY3QsIG9ic2VydmVycywgc2VsZiwgc3RhdGljU2VsZiwgcmVjcmVhdGlvbkxpc3QgfSA9IGN0eDtcbiAgICBjb25zdCBjYiA9IHNlbGYuI29ic2VydmVyQ2FsbGJhY2s7XG4gICAgY29uc3QgeyB0b3AsIHJpZ2h0LCBib3R0b20sIGxlZnQgfSA9IHJlY3Q7XG5cbiAgICBjb25zdCBJbnRlcnNlY3Rpb25PYnNlcnZlckhGID0gc3RhdGljU2VsZi4jSW50ZXJzZWN0aW9uT2JzZXJ2ZXJIRjtcbiAgICBjb25zdCBvcHRpb25zID0gc3RhdGljU2VsZi4jb3B0aW9ucztcblxuICAgIGlmIChyZWNyZWF0aW9uTGlzdC5oYXMoJ3RvcCcpKSB7XG4gICAgICAvLyByZWNyZWF0ZSB0aGUgdG9wIG9ic2VydmVyXG4gICAgICBvcHRpb25zLnJvb3RNYXJnaW4gPSBgMHB4IDBweCAkey0oc3RhdGljU2VsZi4jdmggLSB0b3AgLSAyKX1weCAwcHhgO1xuICAgICAgaWYgKG9ic2VydmVycy50b3ApIG9ic2VydmVycy50b3AudW5vYnNlcnZlKHRhcmdldCk7XG4gICAgICBvYnNlcnZlcnMudG9wID0gbmV3IEludGVyc2VjdGlvbk9ic2VydmVySEYoY2IsIG9wdGlvbnMsIHNlbGYpO1xuICAgIH1cblxuICAgIGlmIChyZWNyZWF0aW9uTGlzdC5oYXMoJ3JpZ2h0JykpIHtcbiAgICAgIC8vIHJlY3JlYXRlIHRoZSByaWdodCBvYnNlcnZlclxuICAgICAgb3B0aW9ucy5yb290TWFyZ2luID0gYDBweCAwcHggMHB4ICR7LShyaWdodCAtIDIpfXB4YDtcbiAgICAgIGlmIChvYnNlcnZlcnMucmlnaHQpIG9ic2VydmVycy5yaWdodC51bm9ic2VydmUodGFyZ2V0KTtcbiAgICAgIG9ic2VydmVycy5yaWdodCA9IG5ldyBJbnRlcnNlY3Rpb25PYnNlcnZlckhGKGNiLCBvcHRpb25zLCBzZWxmKTtcbiAgICB9XG5cbiAgICBpZiAocmVjcmVhdGlvbkxpc3QuaGFzKCdib3R0b20nKSkge1xuICAgICAgLy8gcmVjcmVhdGUgdGhlIGJvdHRvbSBvYnNlcnZlclxuICAgICAgb3B0aW9ucy5yb290TWFyZ2luID0gYCR7LShib3R0b20gLSAyKX1weCAwcHggMHB4IDBweGA7XG4gICAgICBpZiAob2JzZXJ2ZXJzLmJvdHRvbSkgb2JzZXJ2ZXJzLmJvdHRvbS51bm9ic2VydmUodGFyZ2V0KTtcbiAgICAgIG9ic2VydmVycy5ib3R0b20gPSBuZXcgSW50ZXJzZWN0aW9uT2JzZXJ2ZXJIRihjYiwgb3B0aW9ucywgc2VsZik7XG4gICAgfVxuXG4gICAgaWYgKHJlY3JlYXRpb25MaXN0LmhhcygnbGVmdCcpKSB7XG4gICAgICAvLyByZWNyZWF0ZSB0aGUgbGVmdCBvYnNlcnZlclxuICAgICAgb3B0aW9ucy5yb290TWFyZ2luID0gYDBweCAkey0oc3RhdGljU2VsZi4jdncgLSBsZWZ0IC0gMil9cHggMHB4IDBweGA7XG4gICAgICBpZiAob2JzZXJ2ZXJzLmxlZnQpIG9ic2VydmVycy5sZWZ0LnVub2JzZXJ2ZSh0YXJnZXQpO1xuICAgICAgb2JzZXJ2ZXJzLmxlZnQgPSBuZXcgSW50ZXJzZWN0aW9uT2JzZXJ2ZXJIRihjYiwgb3B0aW9ucywgc2VsZik7XG4gICAgfVxuXG4gICAgcmVjcmVhdGlvbkxpc3QuY2xlYXIoKTtcbiAgICBvYnNlcnZlcnMudG9wLm9ic2VydmUodGFyZ2V0KTtcbiAgICBvYnNlcnZlcnMucmlnaHQub2JzZXJ2ZSh0YXJnZXQpO1xuICAgIG9ic2VydmVycy5ib3R0b20ub2JzZXJ2ZSh0YXJnZXQpO1xuICAgIG9ic2VydmVycy5sZWZ0Lm9ic2VydmUodGFyZ2V0KTtcbiAgfVxuXG4gICNjYWxsYmFjaztcbiAgI2N0eDtcblxuICAvKiogQHR5cGUge01hcDxIVE1MRWxlbWVudCwgRm91clNpZGVPYnNlcnZlcj59ICovXG4gICNvYnNlcnZlcnMgPSBuZXcgTWFwKCk7XG5cbiAgLyoqIEB0eXBlIHtNYXA8SFRNTEVsZW1lbnQsIFJlcXVlc3RBbmltYXRpb25GcmFtZUxvb3A+fSAqL1xuICAjcmFmTG9vcHMgPSBuZXcgTWFwKCk7XG5cbiAgLyoqIEB0eXBlIHtNYXA8SFRNTEVsZW1lbnQsIFJBRkxDb250ZXh0Pn0gKi9cbiAgI3JhZkN0eHMgPSBuZXcgTWFwKCk7XG5cbiAgLyoqXG4gICAqIEBwYXJhbSB7QXJyYXk8SW50ZXJzZWN0aW9uT2JzZXJ2ZXJFbnRyeT59IGVudHJpZXNcbiAgICovXG4gICNvYnNlcnZlckNhbGxiYWNrKGVudHJpZXMpIHtcbiAgICAvLyB0aGUgXCJ0aGlzXCIgaGVyZSBpcyBhbiBpbnN0YW5jZSBvZiBJbnRlcnNlY3Rpb25PYnNlcnZlckhGXG5cbiAgICAvKipcbiAgICAgKiBAdHlwZSB7UG9zaXRpb25PYnNlcnZlcn1cbiAgICAgKi9cbiAgICBjb25zdCBzZWxmID0gdGhpcy5jdHg7XG4gICAgY29uc3QgeyB0YXJnZXQsIGJvdW5kaW5nQ2xpZW50UmVjdCB9ID0gZW50cmllc1tlbnRyaWVzLmxlbmd0aCAtIDFdO1xuXG4gICAgY29uc3Qge1xuICAgICAgdG9wOiB0YXJnZXRUb3AsXG4gICAgICByaWdodDogdGFyZ2V0UmlnaHQsXG4gICAgICBib3R0b206IHRhcmdldEJvdHRvbSxcbiAgICAgIGxlZnQ6IHRhcmdldExlZnQsXG4gICAgfSA9IGJvdW5kaW5nQ2xpZW50UmVjdDtcblxuICAgIGNvbnN0IG9ic2VydmVycyA9IHNlbGYuI29ic2VydmVycy5nZXQodGFyZ2V0KTtcbiAgICBjb25zdCByZWNyZWF0aW9uTGlzdCA9IHNlbGYuI3JhZkN0eHMuZ2V0KHRhcmdldCkucmVjcmVhdGlvbkxpc3Q7XG5cbiAgICBjb25zdCB0b3BSZWNvcmRzID1cbiAgICAgIHRoaXMgPT09IG9ic2VydmVycy50b3AgPyBlbnRyaWVzIDogb2JzZXJ2ZXJzLnRvcC50YWtlUmVjb3JkcygpO1xuICAgIGNvbnN0IHJpZ2h0UmVjb3JkcyA9XG4gICAgICB0aGlzID09PSBvYnNlcnZlcnMucmlnaHQgPyBlbnRyaWVzIDogb2JzZXJ2ZXJzLnJpZ2h0LnRha2VSZWNvcmRzKCk7XG4gICAgY29uc3QgYm90dG9tUmVjb3JkcyA9XG4gICAgICB0aGlzID09PSBvYnNlcnZlcnMuYm90dG9tID8gZW50cmllcyA6IG9ic2VydmVycy5ib3R0b20udGFrZVJlY29yZHMoKTtcbiAgICBjb25zdCBsZWZ0UmVjb3JkcyA9XG4gICAgICB0aGlzID09PSBvYnNlcnZlcnMubGVmdCA/IGVudHJpZXMgOiBvYnNlcnZlcnMubGVmdC50YWtlUmVjb3JkcygpO1xuXG4gICAgLy8gZGlzcGxheTogbm9uZTsgcHJvdGVjdGlvbiBhZ2FpbnN0IGFuIGluZmluaXRlIGxvb3BcbiAgICBpZiAoIXRhcmdldC5vZmZzZXRQYXJlbnQpIHtcbiAgICAgIHNlbGYuI2NhbGxiYWNrKHRhcmdldCwgYm91bmRpbmdDbGllbnRSZWN0LCBzZWxmLiNjdHgpO1xuICAgICAgcmV0dXJuO1xuICAgIH1cblxuICAgIGlmICh0b3BSZWNvcmRzLmxlbmd0aCkge1xuICAgICAgY29uc3QgeyBib3R0b206IHJvb3RCb3R0b20sIHdpZHRoOiByb290V2lkdGggfSA9XG4gICAgICAgIHRvcFJlY29yZHNbdG9wUmVjb3Jkcy5sZW5ndGggLSAxXS5yb290Qm91bmRzO1xuXG4gICAgICBQb3NpdGlvbk9ic2VydmVyLiN2dyA9IHJvb3RXaWR0aDtcblxuICAgICAgY29uc3QgaW50ZXJzZWN0aW9uSGVpZ2h0ID0gcm9vdEJvdHRvbSAtIHRhcmdldFRvcDtcblxuICAgICAgaWYgKCEoMSA8IGludGVyc2VjdGlvbkhlaWdodCAmJiBpbnRlcnNlY3Rpb25IZWlnaHQgPCAzKSkge1xuICAgICAgICBpZiAodGFyZ2V0VG9wID49IDApIHtcbiAgICAgICAgICByZWNyZWF0aW9uTGlzdC5hZGQoJ3RvcCcpO1xuICAgICAgICB9XG4gICAgICB9XG4gICAgfVxuXG4gICAgaWYgKHJpZ2h0UmVjb3Jkcy5sZW5ndGgpIHtcbiAgICAgIGNvbnN0IHtcbiAgICAgICAgcmlnaHQ6IHJvb3RSaWdodCxcbiAgICAgICAgbGVmdDogcm9vdExlZnQsXG4gICAgICAgIGhlaWdodDogcm9vdEhlaWdodCxcbiAgICAgIH0gPSByaWdodFJlY29yZHNbcmlnaHRSZWNvcmRzLmxlbmd0aCAtIDFdLnJvb3RCb3VuZHM7XG5cbiAgICAgIFBvc2l0aW9uT2JzZXJ2ZXIuI3ZoID0gcm9vdEhlaWdodDtcbiAgICAgIFBvc2l0aW9uT2JzZXJ2ZXIuI3Z3ID0gcm9vdFJpZ2h0O1xuXG4gICAgICBjb25zdCBpbnRlcnNlY3Rpb25XaWR0aCA9IHRhcmdldFJpZ2h0IC0gcm9vdExlZnQ7XG5cbiAgICAgIGlmICghKDEgPCBpbnRlcnNlY3Rpb25XaWR0aCAmJiBpbnRlcnNlY3Rpb25XaWR0aCA8IDMpKSB7XG4gICAgICAgIGlmICh0YXJnZXRSaWdodCA8PSByb290UmlnaHQpIHtcbiAgICAgICAgICByZWNyZWF0aW9uTGlzdC5hZGQoJ3JpZ2h0Jyk7XG4gICAgICAgIH1cbiAgICAgIH1cbiAgICB9XG5cbiAgICBpZiAoYm90dG9tUmVjb3Jkcy5sZW5ndGgpIHtcbiAgICAgIGNvbnN0IHtcbiAgICAgICAgdG9wOiByb290VG9wLFxuICAgICAgICBib3R0b206IHJvb3RCb3R0b20sXG4gICAgICAgIHdpZHRoOiByb290V2lkdGgsXG4gICAgICB9ID0gYm90dG9tUmVjb3Jkc1tib3R0b21SZWNvcmRzLmxlbmd0aCAtIDFdLnJvb3RCb3VuZHM7XG5cbiAgICAgIFBvc2l0aW9uT2JzZXJ2ZXIuI3Z3ID0gcm9vdFdpZHRoO1xuICAgICAgUG9zaXRpb25PYnNlcnZlci4jdmggPSByb290Qm90dG9tO1xuXG4gICAgICBjb25zdCBpbnRlcnNlY3Rpb25IZWlnaHQgPSB0YXJnZXRCb3R0b20gLSByb290VG9wO1xuXG4gICAgICBpZiAoISgxIDwgaW50ZXJzZWN0aW9uSGVpZ2h0ICYmIGludGVyc2VjdGlvbkhlaWdodCA8IDMpKSB7XG4gICAgICAgIGlmICh0YXJnZXRCb3R0b20gPD0gcm9vdEJvdHRvbSkge1xuICAgICAgICAgIHJlY3JlYXRpb25MaXN0LmFkZCgnYm90dG9tJyk7XG4gICAgICAgIH1cbiAgICAgIH1cbiAgICB9XG5cbiAgICBpZiAobGVmdFJlY29yZHMubGVuZ3RoKSB7XG4gICAgICBjb25zdCB7IHJpZ2h0OiByb290UmlnaHQsIGhlaWdodDogcm9vdEhlaWdodCB9ID1cbiAgICAgICAgbGVmdFJlY29yZHNbbGVmdFJlY29yZHMubGVuZ3RoIC0gMV0ucm9vdEJvdW5kcztcblxuICAgICAgUG9zaXRpb25PYnNlcnZlci4jdmggPSByb290SGVpZ2h0O1xuXG4gICAgICBjb25zdCBpbnRlcnNlY3Rpb25XaWR0aCA9IHJvb3RSaWdodCAtIHRhcmdldExlZnQ7XG5cbiAgICAgIGlmICghKDEgPCBpbnRlcnNlY3Rpb25XaWR0aCAmJiBpbnRlcnNlY3Rpb25XaWR0aCA8IDMpKSB7XG4gICAgICAgIGlmICh0YXJnZXRMZWZ0ID49IDApIHtcbiAgICAgICAgICByZWNyZWF0aW9uTGlzdC5hZGQoJ2xlZnQnKTtcbiAgICAgICAgfVxuICAgICAgfVxuICAgIH1cblxuICAgIGlmIChyZWNyZWF0aW9uTGlzdC5zaXplKSB7XG4gICAgICBzZWxmLiNjYWxsYmFjayh0YXJnZXQsIGJvdW5kaW5nQ2xpZW50UmVjdCwgc2VsZi4jY3R4KTtcbiAgICAgIHNlbGYuI3JhZkxvb3BzLmdldCh0YXJnZXQpLnN0YXJ0KCk7XG4gICAgfVxuICB9XG5cbiAgLyoqXG4gICAqIEBwYXJhbSB7UG9zaXRpb25PYnNlcnZlckNhbGxiYWNrfSBjYWxsYmFja1xuICAgKiBAcGFyYW0geyp9IGN0eFxuICAgKi9cbiAgY29uc3RydWN0b3IoY2FsbGJhY2ssIGN0eCkge1xuICAgIHRoaXMuI2NhbGxiYWNrID0gY2FsbGJhY2s7XG4gICAgdGhpcy4jY3R4ID0gY3R4O1xuICB9XG5cbiAgLyoqXG4gICAqIEBwYXJhbSB7SFRNTEVsZW1lbnR9IHRhcmdldFxuICAgKi9cbiAgb2JzZXJ2ZSh0YXJnZXQpIHtcbiAgICBpZiAodGhpcy4jb2JzZXJ2ZXJzLmhhcyh0YXJnZXQpKSByZXR1cm47XG5cbiAgICBjb25zdCBvYnNlcnZlcnMgPSB7IHRvcDogbnVsbCwgcmlnaHQ6IG51bGwsIGJvdHRvbTogbnVsbCwgbGVmdDogbnVsbCB9O1xuICAgIGNvbnN0IHRhcmdldFJlY3QgPSB0YXJnZXQuZ2V0Qm91bmRpbmdDbGllbnRSZWN0KCk7XG5cbiAgICAvKipcbiAgICAgKiBAdHlwZSB7UkFGTENvbnRleHR9XG4gICAgICovXG4gICAgY29uc3QgY3R4ID0ge1xuICAgICAgdGFyZ2V0LFxuICAgICAgb2JzZXJ2ZXJzLFxuICAgICAgcmVjdDoge1xuICAgICAgICB0b3A6IHRhcmdldFJlY3QudG9wLFxuICAgICAgICByaWdodDogdGFyZ2V0UmVjdC5yaWdodCxcbiAgICAgICAgYm90dG9tOiB0YXJnZXRSZWN0LmJvdHRvbSxcbiAgICAgICAgbGVmdDogdGFyZ2V0UmVjdC5sZWZ0LFxuICAgICAgfSxcbiAgICAgIHNlbGY6IHRoaXMsXG4gICAgICBzdGF0aWNTZWxmOiBQb3NpdGlvbk9ic2VydmVyLFxuICAgICAgcmVjcmVhdGlvbkxpc3Q6IG5ldyBTZXQoWyd0b3AnLCAncmlnaHQnLCAnYm90dG9tJywgJ2xlZnQnXSksXG4gICAgfTtcblxuICAgIGNvbnN0IHJhZkxvb3AgPSBuZXcgUG9zaXRpb25PYnNlcnZlci4jUmVxdWVzdEFuaW1hdGlvbkZyYW1lTG9vcChjdHgpXG4gICAgICAuc3RhcnRlZChQb3NpdGlvbk9ic2VydmVyLiN1bm9ic2VydmUpXG4gICAgICAuZWFjaChQb3NpdGlvbk9ic2VydmVyLiNwb3NpdGlvbkNoYW5naW5nKVxuICAgICAgLnN0b3BwZWQoUG9zaXRpb25PYnNlcnZlci4jY3JlYXRlT2JzZXJ2ZXIpO1xuXG4gICAgUG9zaXRpb25PYnNlcnZlci4jY3JlYXRlT2JzZXJ2ZXIoY3R4KTtcblxuICAgIHRoaXMuI29ic2VydmVycy5zZXQodGFyZ2V0LCBvYnNlcnZlcnMpO1xuICAgIHRoaXMuI3JhZkxvb3BzLnNldCh0YXJnZXQsIHJhZkxvb3ApO1xuICAgIHRoaXMuI3JhZkN0eHMuc2V0KHRhcmdldCwgY3R4KTtcblxuICAgIHRoaXMuI2NhbGxiYWNrKHRhcmdldCwgdGFyZ2V0UmVjdCwgdGhpcy4jY3R4KTsgLy8gaW5pdGlhbCBjYWxsYmFjayBpbnZva2F0aW9uXG4gIH1cblxuICAvKipcbiAgICogQHBhcmFtIHtIVE1MRWxlbWVudH0gdGFyZ2V0XG4gICAqL1xuICB1bm9ic2VydmUodGFyZ2V0KSB7XG4gICAgaWYgKCF0aGlzLiNvYnNlcnZlcnMuaGFzKHRhcmdldCkpIHJldHVybjtcblxuICAgIGNvbnN0IG9ic2VydmVycyA9IHRoaXMuI29ic2VydmVycy5nZXQodGFyZ2V0KTtcbiAgICBjb25zdCBjdHggPSB7IHRhcmdldCwgb2JzZXJ2ZXJzIH07XG5cbiAgICBQb3NpdGlvbk9ic2VydmVyLiN1bm9ic2VydmUoY3R4KTtcblxuICAgIHRoaXMuI29ic2VydmVycy5kZWxldGUodGFyZ2V0KTtcbiAgICB0aGlzLiNyYWZMb29wcy5kZWxldGUodGFyZ2V0KTtcbiAgICB0aGlzLiNyYWZDdHhzLmRlbGV0ZSh0YXJnZXQpO1xuICB9XG5cbiAgZGlzY29ubmVjdCgpIHtcbiAgICB0aGlzLiNvYnNlcnZlcnMuZm9yRWFjaCgoXywgdGFyZ2V0KSA9PiB0aGlzLnVub2JzZXJ2ZSh0YXJnZXQpKTtcbiAgfVxuXG4gIC8qKlxuICAgKiBAcmV0dXJucyB7TWFwSXRlcmF0b3I8SFRNTEVsZW1lbnQ+fVxuICAgKi9cbiAgZ2V0VGFyZ2V0cygpIHtcbiAgICByZXR1cm4gdGhpcy4jb2JzZXJ2ZXJzLmtleXMoKTtcbiAgfVxufVxuXG5leHBvcnQgeyBQb3NpdGlvbk9ic2VydmVyIGFzIGRlZmF1bHQgfTtcbiIsIi8qKlxuICogQGNhbGxiYWNrIFJlcXVlc3RBbmltYXRpb25GcmFtZVN0YXRlQ2FsbGJhY2tcbiAqIEBwYXJhbSB7Kn0gY3R4XG4gKiBAcGFyYW0ge1JlcXVlc3RBbmltYXRpb25GcmFtZUxvb3B9IGxvb3BcbiAqIEBwYXJhbSB7bnVtYmVyfSB0aW1lc3RhbXBcbiAqL1xuXG5jbGFzcyBSZXF1ZXN0QW5pbWF0aW9uRnJhbWVMb29wIHtcbiAgI2xvb3BJRDtcbiAgI2N0eDtcbiAgI3N0YXJ0ZWRDQiA9IEZ1bmN0aW9uLnByb3RvdHlwZTtcbiAgI2VhY2hDQiA9IEZ1bmN0aW9uLnByb3RvdHlwZTtcbiAgI3N0b3BwZWRDQiA9IEZ1bmN0aW9uLnByb3RvdHlwZTtcblxuICAjckFGQ2FsbGJhY2sgPSAodGltZXN0YW1wKSA9PiB7XG4gICAgdGhpcy4jbG9vcElEID0gcmVxdWVzdEFuaW1hdGlvbkZyYW1lKHRoaXMuI3JBRkNhbGxiYWNrKTtcbiAgICB0aGlzLiNlYWNoQ0IodGhpcy4jY3R4LCB0aGlzLCB0aW1lc3RhbXApO1xuICB9O1xuXG4gIGNvbnN0cnVjdG9yKGN0eCkge1xuICAgIHRoaXMuI2N0eCA9IGN0eDtcbiAgfVxuXG4gIC8qKlxuICAgKiBAcGFyYW0ge1JlcXVlc3RBbmltYXRpb25GcmFtZVN0YXRlQ2FsbGJhY2t9IGNiXG4gICAqIEByZXR1cm5zIHtSZXF1ZXN0QW5pbWF0aW9uRnJhbWVMb29wfVxuICAgKi9cbiAgc3RhcnRlZChjYikge1xuICAgIHRoaXMuI3N0YXJ0ZWRDQiA9IGNiO1xuICAgIHJldHVybiB0aGlzO1xuICB9XG5cbiAgLyoqXG4gICAqIEBwYXJhbSB7UmVxdWVzdEFuaW1hdGlvbkZyYW1lU3RhdGVDYWxsYmFja30gY2JcbiAgICogQHJldHVybnMge1JlcXVlc3RBbmltYXRpb25GcmFtZUxvb3B9XG4gICAqL1xuICBlYWNoKGNiKSB7XG4gICAgdGhpcy4jZWFjaENCID0gY2I7XG4gICAgcmV0dXJuIHRoaXM7XG4gIH1cblxuICAvKipcbiAgICogQHBhcmFtIHtSZXF1ZXN0QW5pbWF0aW9uRnJhbWVTdGF0ZUNhbGxiYWNrfSBjYlxuICAgKiBAcmV0dXJucyB7UmVxdWVzdEFuaW1hdGlvbkZyYW1lTG9vcH1cbiAgICovXG4gIHN0b3BwZWQoY2IpIHtcbiAgICB0aGlzLiNzdG9wcGVkQ0IgPSBjYjtcbiAgICByZXR1cm4gdGhpcztcbiAgfVxuXG4gIHN0YXJ0KCkge1xuICAgIGlmICh0aGlzLiNsb29wSUQgPT09IHVuZGVmaW5lZCkge1xuICAgICAgdGhpcy4jbG9vcElEID0gcmVxdWVzdEFuaW1hdGlvbkZyYW1lKHRoaXMuI3JBRkNhbGxiYWNrKTtcbiAgICAgIHRoaXMuI3N0YXJ0ZWRDQih0aGlzLiNjdHgsIHRoaXMsIDApO1xuICAgIH1cbiAgfVxuXG4gIHN0b3AoKSB7XG4gICAgY2FuY2VsQW5pbWF0aW9uRnJhbWUodGhpcy4jbG9vcElEKTtcbiAgICB0aGlzLiNzdG9wcGVkQ0IodGhpcy4jY3R4LCB0aGlzLCAwKTtcbiAgICB0aGlzLiNsb29wSUQgPSB1bmRlZmluZWQ7XG4gIH1cbn1cblxuZXhwb3J0IHsgUmVxdWVzdEFuaW1hdGlvbkZyYW1lTG9vcCBhcyBkZWZhdWx0IH07XG4iLCJpbXBvcnQgUG9zaXRpb25PYnNlcnZlciBmcm9tIFwiQGl0aWhvbi9wb3NpdGlvbi1vYnNlcnZlclwiO1xubGV0IG5leHREb21JZCA9IDE7XG5sZXQgdW5yZXBvcnRlZENoYW5nZXMgPSBuZXcgTWFwKCk7XG5sZXQgcmVwb3J0Q2FsbGJhY2sgPSBudWxsO1xuZnVuY3Rpb24gZXEocmVjdCwgb3RoZXIpIHtcbiAgICByZXR1cm4gcmVjdC54ID09IG90aGVyLnggJiYgcmVjdC55ID09IG90aGVyLnkgJiYgcmVjdC53aWR0aCA9PSBvdGhlci53aWR0aCAmJiByZWN0LmhlaWdodCA9PSBvdGhlci5oZWlnaHQ7XG59XG5mdW5jdGlvbiByZXBvcnRJZlBvc3NpYmxlKCkge1xuICAgIGlmIChyZXBvcnRDYWxsYmFjayAhPSBudWxsICYmIHVucmVwb3J0ZWRDaGFuZ2VzLnNpemUgPiAwKSB7XG4gICAgICAgIHJlcG9ydENhbGxiYWNrKEFycmF5LmZyb20odW5yZXBvcnRlZENoYW5nZXMudmFsdWVzKCkpKTtcbiAgICAgICAgdW5yZXBvcnRlZENoYW5nZXMuY2xlYXIoKTtcbiAgICAgICAgcmVwb3J0Q2FsbGJhY2sgPSBudWxsO1xuICAgIH1cbn1cbmZ1bmN0aW9uIHJlcG9ydENoYW5nZWQodGFyZ2V0LCB0YXJnZXRJZCwgbmV3UmVjdCkge1xuICAgIGNvbnN0IHNlcmlhbGl6ZWQgPSBuZXdSZWN0ID8gKHtcbiAgICAgICAgeDogbmV3UmVjdC54LFxuICAgICAgICB5OiBuZXdSZWN0LnksXG4gICAgICAgIHdpZHRoOiBuZXdSZWN0LndpZHRoLFxuICAgICAgICBoZWlnaHQ6IG5ld1JlY3QuaGVpZ2h0LFxuICAgICAgICBjYXB0dXJlU2Nyb2xsOiB0YXJnZXQuY2xhc3NMaXN0LmNvbnRhaW5zKFwiYnJvd3NlclNjcm9sbENhcHR1cmVcIilcbiAgICB9KSA6IG51bGw7XG4gICAgY29uc3Qgb2xkU2VyaWFsaXplZCA9IHRhcmdldC5sYXN0QnJvd3NlclJlY3Q7XG4gICAgaWYgKG9sZFNlcmlhbGl6ZWQgPT09IHVuZGVmaW5lZCB8fCAoKHNlcmlhbGl6ZWQgPT09IG51bGwpICE9PSAob2xkU2VyaWFsaXplZCA9PT0gbnVsbCkpIHx8IChzZXJpYWxpemVkICE9IG51bGwgJiYgb2xkU2VyaWFsaXplZCAhPT0gbnVsbCAmJiAhZXEoc2VyaWFsaXplZCwgb2xkU2VyaWFsaXplZCkpKSB7XG4gICAgICAgIHVucmVwb3J0ZWRDaGFuZ2VzLnNldCh0YXJnZXRJZCwgKHtcbiAgICAgICAgICAgIHRhcmdldElkLFxuICAgICAgICAgICAgbmV3UmVjdDogc2VyaWFsaXplZCxcbiAgICAgICAgfSkpO1xuICAgICAgICB0YXJnZXQubGFzdEJyb3dzZXJSZWN0ID0gc2VyaWFsaXplZDtcbiAgICAgICAgcmVwb3J0SWZQb3NzaWJsZSgpO1xuICAgIH1cbn1cbmZ1bmN0aW9uIGdldERvbUlkKGVsZW1lbnQpIHtcbiAgICBjb25zdCBpZCA9IGVsZW1lbnQuYnJvd3NlckRvbUlkO1xuICAgIGlmIChpZCkge1xuICAgICAgICByZXR1cm4gaWQ7XG4gICAgfVxuICAgIGVsc2Uge1xuICAgICAgICByZXR1cm4gbnVsbDtcbiAgICB9XG59XG5mdW5jdGlvbiBlbnN1cmVEb21JZChlbGVtZW50KSB7XG4gICAgY29uc3QgZXhpc3RpbmcgPSBnZXREb21JZChlbGVtZW50KTtcbiAgICBpZiAoZXhpc3RpbmcpIHtcbiAgICAgICAgcmV0dXJuIGV4aXN0aW5nO1xuICAgIH1cbiAgICBuZXh0RG9tSWQgKz0gMTtcbiAgICBlbGVtZW50LmJyb3dzZXJEb21JZCA9IG5leHREb21JZDtcbiAgICByZXR1cm4gbmV4dERvbUlkO1xufVxuZnVuY3Rpb24gb25Cb3VuZHNDaGFuZ2VkKHRhcmdldCwgdGFyZ2V0UmVjdCwgY3R4KSB7XG4gICAgaWYgKHRhcmdldCBpbnN0YW5jZW9mIEhUTUxFbGVtZW50KSB7XG4gICAgICAgIGNvbnN0IGRvbUlkID0gZ2V0RG9tSWQodGFyZ2V0KTtcbiAgICAgICAgaWYgKGRvbUlkICE9IG51bGwpIHtcbiAgICAgICAgICAgIHJlcG9ydENoYW5nZWQodGFyZ2V0LCBkb21JZCwgdGFyZ2V0UmVjdCk7XG4gICAgICAgIH1cbiAgICB9XG59XG5jb25zdCBwb3NpdGlvbk9ic2VydmVyID0gbmV3IFBvc2l0aW9uT2JzZXJ2ZXIob25Cb3VuZHNDaGFuZ2VkLCB7fSk7XG5mdW5jdGlvbiBzdGFydE9ic2VydmluZyhjaGlsZE5vZGUpIHtcbiAgICBjb25zdCBkb21JZCA9IGVuc3VyZURvbUlkKGNoaWxkTm9kZSk7XG4gICAgcG9zaXRpb25PYnNlcnZlci5vYnNlcnZlKGNoaWxkTm9kZSk7XG4gICAgcmVwb3J0Q2hhbmdlZChjaGlsZE5vZGUsIGRvbUlkLCBjaGlsZE5vZGUuZ2V0Qm91bmRpbmdDbGllbnRSZWN0KCkpO1xufVxuY29uc3Qgb2JzZXJ2ZXIgPSBuZXcgTXV0YXRpb25PYnNlcnZlcihtdXRhdGlvbnMgPT4ge1xuICAgIGZvciAoY29uc3QgbXV0YXRpb24gb2YgbXV0YXRpb25zKSB7XG4gICAgICAgIGlmIChtdXRhdGlvbi50eXBlID09IFwiY2hpbGRMaXN0XCIpIHtcbiAgICAgICAgICAgIGZvciAoY29uc3QgY2hpbGROb2RlIG9mIG11dGF0aW9uLmFkZGVkTm9kZXMpIHtcbiAgICAgICAgICAgICAgICBpZiAoY2hpbGROb2RlIGluc3RhbmNlb2YgSFRNTEVsZW1lbnQpIHtcbiAgICAgICAgICAgICAgICAgICAgaWYgKGNoaWxkTm9kZS5jbGFzc0xpc3QuY29udGFpbnMoXCJicm93c2VyQ2xpY2tDYXB0dXJlXCIpKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBzdGFydE9ic2VydmluZyhjaGlsZE5vZGUpO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZm9yIChjb25zdCBjaGlsZE5vZGUgb2YgbXV0YXRpb24ucmVtb3ZlZE5vZGVzKSB7XG4gICAgICAgICAgICAgICAgaWYgKGNoaWxkTm9kZSBpbnN0YW5jZW9mIEhUTUxFbGVtZW50KSB7XG4gICAgICAgICAgICAgICAgICAgIGNvbnN0IGRvbUlkID0gZ2V0RG9tSWQoY2hpbGROb2RlKTtcbiAgICAgICAgICAgICAgICAgICAgaWYgKGRvbUlkICE9IG51bGwpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHBvc2l0aW9uT2JzZXJ2ZXIudW5vYnNlcnZlKGNoaWxkTm9kZSk7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXBvcnRDaGFuZ2VkKGNoaWxkTm9kZSwgZG9tSWQsIG51bGwpO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgfVxufSk7XG5mb3IgKGNvbnN0IGV4aXN0aW5nIG9mIGRvY3VtZW50LnF1ZXJ5U2VsZWN0b3JBbGwoXCIuYnJvd3NlckNsaWNrQ2FwdHVyZVwiKSkge1xuICAgIGlmIChleGlzdGluZyBpbnN0YW5jZW9mIEhUTUxFbGVtZW50KSB7XG4gICAgICAgIHN0YXJ0T2JzZXJ2aW5nKGV4aXN0aW5nKTtcbiAgICB9XG59XG5vYnNlcnZlci5vYnNlcnZlKGRvY3VtZW50LmJvZHksIHsgY2hpbGRMaXN0OiB0cnVlLCBzdWJ0cmVlOiB0cnVlIH0pO1xud2luZG93LnJlZ2lzdGVyUmVwb3J0Q2FsbGJhY2sgPSBmdW5jdGlvbiAoY2IpIHtcbiAgICByZXBvcnRDYWxsYmFjayA9IGNiO1xuICAgIHJlcG9ydElmUG9zc2libGUoKTtcbn07XG4iLCIvLyBUaGUgbW9kdWxlIGNhY2hlXG52YXIgX193ZWJwYWNrX21vZHVsZV9jYWNoZV9fID0ge307XG5cbi8vIFRoZSByZXF1aXJlIGZ1bmN0aW9uXG5mdW5jdGlvbiBfX3dlYnBhY2tfcmVxdWlyZV9fKG1vZHVsZUlkKSB7XG5cdC8vIENoZWNrIGlmIG1vZHVsZSBpcyBpbiBjYWNoZVxuXHR2YXIgY2FjaGVkTW9kdWxlID0gX193ZWJwYWNrX21vZHVsZV9jYWNoZV9fW21vZHVsZUlkXTtcblx0aWYgKGNhY2hlZE1vZHVsZSAhPT0gdW5kZWZpbmVkKSB7XG5cdFx0cmV0dXJuIGNhY2hlZE1vZHVsZS5leHBvcnRzO1xuXHR9XG5cdC8vIENyZWF0ZSBhIG5ldyBtb2R1bGUgKGFuZCBwdXQgaXQgaW50byB0aGUgY2FjaGUpXG5cdHZhciBtb2R1bGUgPSBfX3dlYnBhY2tfbW9kdWxlX2NhY2hlX19bbW9kdWxlSWRdID0ge1xuXHRcdC8vIG5vIG1vZHVsZS5pZCBuZWVkZWRcblx0XHQvLyBubyBtb2R1bGUubG9hZGVkIG5lZWRlZFxuXHRcdGV4cG9ydHM6IHt9XG5cdH07XG5cblx0Ly8gRXhlY3V0ZSB0aGUgbW9kdWxlIGZ1bmN0aW9uXG5cdF9fd2VicGFja19tb2R1bGVzX19bbW9kdWxlSWRdKG1vZHVsZSwgbW9kdWxlLmV4cG9ydHMsIF9fd2VicGFja19yZXF1aXJlX18pO1xuXG5cdC8vIFJldHVybiB0aGUgZXhwb3J0cyBvZiB0aGUgbW9kdWxlXG5cdHJldHVybiBtb2R1bGUuZXhwb3J0cztcbn1cblxuIiwidmFyIHdlYnBhY2tRdWV1ZXMgPSB0eXBlb2YgU3ltYm9sID09PSBcImZ1bmN0aW9uXCIgPyBTeW1ib2woXCJ3ZWJwYWNrIHF1ZXVlc1wiKSA6IFwiX193ZWJwYWNrX3F1ZXVlc19fXCI7XG52YXIgd2VicGFja0V4cG9ydHMgPSB0eXBlb2YgU3ltYm9sID09PSBcImZ1bmN0aW9uXCIgPyBTeW1ib2woXCJ3ZWJwYWNrIGV4cG9ydHNcIikgOiBcIl9fd2VicGFja19leHBvcnRzX19cIjtcbnZhciB3ZWJwYWNrRXJyb3IgPSB0eXBlb2YgU3ltYm9sID09PSBcImZ1bmN0aW9uXCIgPyBTeW1ib2woXCJ3ZWJwYWNrIGVycm9yXCIpIDogXCJfX3dlYnBhY2tfZXJyb3JfX1wiO1xudmFyIHJlc29sdmVRdWV1ZSA9IChxdWV1ZSkgPT4ge1xuXHRpZihxdWV1ZSAmJiBxdWV1ZS5kIDwgMSkge1xuXHRcdHF1ZXVlLmQgPSAxO1xuXHRcdHF1ZXVlLmZvckVhY2goKGZuKSA9PiAoZm4uci0tKSk7XG5cdFx0cXVldWUuZm9yRWFjaCgoZm4pID0+IChmbi5yLS0gPyBmbi5yKysgOiBmbigpKSk7XG5cdH1cbn1cbnZhciB3cmFwRGVwcyA9IChkZXBzKSA9PiAoZGVwcy5tYXAoKGRlcCkgPT4ge1xuXHRpZihkZXAgIT09IG51bGwgJiYgdHlwZW9mIGRlcCA9PT0gXCJvYmplY3RcIikge1xuXHRcdGlmKGRlcFt3ZWJwYWNrUXVldWVzXSkgcmV0dXJuIGRlcDtcblx0XHRpZihkZXAudGhlbikge1xuXHRcdFx0dmFyIHF1ZXVlID0gW107XG5cdFx0XHRxdWV1ZS5kID0gMDtcblx0XHRcdGRlcC50aGVuKChyKSA9PiB7XG5cdFx0XHRcdG9ialt3ZWJwYWNrRXhwb3J0c10gPSByO1xuXHRcdFx0XHRyZXNvbHZlUXVldWUocXVldWUpO1xuXHRcdFx0fSwgKGUpID0+IHtcblx0XHRcdFx0b2JqW3dlYnBhY2tFcnJvcl0gPSBlO1xuXHRcdFx0XHRyZXNvbHZlUXVldWUocXVldWUpO1xuXHRcdFx0fSk7XG5cdFx0XHR2YXIgb2JqID0ge307XG5cdFx0XHRvYmpbd2VicGFja1F1ZXVlc10gPSAoZm4pID0+IChmbihxdWV1ZSkpO1xuXHRcdFx0cmV0dXJuIG9iajtcblx0XHR9XG5cdH1cblx0dmFyIHJldCA9IHt9O1xuXHRyZXRbd2VicGFja1F1ZXVlc10gPSB4ID0+IHt9O1xuXHRyZXRbd2VicGFja0V4cG9ydHNdID0gZGVwO1xuXHRyZXR1cm4gcmV0O1xufSkpO1xuX193ZWJwYWNrX3JlcXVpcmVfXy5hID0gKG1vZHVsZSwgYm9keSwgaGFzQXdhaXQpID0+IHtcblx0dmFyIHF1ZXVlO1xuXHRoYXNBd2FpdCAmJiAoKHF1ZXVlID0gW10pLmQgPSAtMSk7XG5cdHZhciBkZXBRdWV1ZXMgPSBuZXcgU2V0KCk7XG5cdHZhciBleHBvcnRzID0gbW9kdWxlLmV4cG9ydHM7XG5cdHZhciBjdXJyZW50RGVwcztcblx0dmFyIG91dGVyUmVzb2x2ZTtcblx0dmFyIHJlamVjdDtcblx0dmFyIHByb21pc2UgPSBuZXcgUHJvbWlzZSgocmVzb2x2ZSwgcmVqKSA9PiB7XG5cdFx0cmVqZWN0ID0gcmVqO1xuXHRcdG91dGVyUmVzb2x2ZSA9IHJlc29sdmU7XG5cdH0pO1xuXHRwcm9taXNlW3dlYnBhY2tFeHBvcnRzXSA9IGV4cG9ydHM7XG5cdHByb21pc2Vbd2VicGFja1F1ZXVlc10gPSAoZm4pID0+IChxdWV1ZSAmJiBmbihxdWV1ZSksIGRlcFF1ZXVlcy5mb3JFYWNoKGZuKSwgcHJvbWlzZVtcImNhdGNoXCJdKHggPT4ge30pKTtcblx0bW9kdWxlLmV4cG9ydHMgPSBwcm9taXNlO1xuXHRib2R5KChkZXBzKSA9PiB7XG5cdFx0Y3VycmVudERlcHMgPSB3cmFwRGVwcyhkZXBzKTtcblx0XHR2YXIgZm47XG5cdFx0dmFyIGdldFJlc3VsdCA9ICgpID0+IChjdXJyZW50RGVwcy5tYXAoKGQpID0+IHtcblx0XHRcdGlmKGRbd2VicGFja0Vycm9yXSkgdGhyb3cgZFt3ZWJwYWNrRXJyb3JdO1xuXHRcdFx0cmV0dXJuIGRbd2VicGFja0V4cG9ydHNdO1xuXHRcdH0pKVxuXHRcdHZhciBwcm9taXNlID0gbmV3IFByb21pc2UoKHJlc29sdmUpID0+IHtcblx0XHRcdGZuID0gKCkgPT4gKHJlc29sdmUoZ2V0UmVzdWx0KSk7XG5cdFx0XHRmbi5yID0gMDtcblx0XHRcdHZhciBmblF1ZXVlID0gKHEpID0+IChxICE9PSBxdWV1ZSAmJiAhZGVwUXVldWVzLmhhcyhxKSAmJiAoZGVwUXVldWVzLmFkZChxKSwgcSAmJiAhcS5kICYmIChmbi5yKyssIHEucHVzaChmbikpKSk7XG5cdFx0XHRjdXJyZW50RGVwcy5tYXAoKGRlcCkgPT4gKGRlcFt3ZWJwYWNrUXVldWVzXShmblF1ZXVlKSkpO1xuXHRcdH0pO1xuXHRcdHJldHVybiBmbi5yID8gcHJvbWlzZSA6IGdldFJlc3VsdCgpO1xuXHR9LCAoZXJyKSA9PiAoKGVyciA/IHJlamVjdChwcm9taXNlW3dlYnBhY2tFcnJvcl0gPSBlcnIpIDogb3V0ZXJSZXNvbHZlKGV4cG9ydHMpKSwgcmVzb2x2ZVF1ZXVlKHF1ZXVlKSkpO1xuXHRxdWV1ZSAmJiBxdWV1ZS5kIDwgMCAmJiAocXVldWUuZCA9IDApO1xufTsiLCIvLyBkZWZpbmUgZ2V0dGVyIGZ1bmN0aW9ucyBmb3IgaGFybW9ueSBleHBvcnRzXG5fX3dlYnBhY2tfcmVxdWlyZV9fLmQgPSAoZXhwb3J0cywgZGVmaW5pdGlvbikgPT4ge1xuXHRmb3IodmFyIGtleSBpbiBkZWZpbml0aW9uKSB7XG5cdFx0aWYoX193ZWJwYWNrX3JlcXVpcmVfXy5vKGRlZmluaXRpb24sIGtleSkgJiYgIV9fd2VicGFja19yZXF1aXJlX18ubyhleHBvcnRzLCBrZXkpKSB7XG5cdFx0XHRPYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywga2V5LCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZGVmaW5pdGlvbltrZXldIH0pO1xuXHRcdH1cblx0fVxufTsiLCJfX3dlYnBhY2tfcmVxdWlyZV9fLm8gPSAob2JqLCBwcm9wKSA9PiAoT2JqZWN0LnByb3RvdHlwZS5oYXNPd25Qcm9wZXJ0eS5jYWxsKG9iaiwgcHJvcCkpIiwiLy8gZGVmaW5lIF9fZXNNb2R1bGUgb24gZXhwb3J0c1xuX193ZWJwYWNrX3JlcXVpcmVfXy5yID0gKGV4cG9ydHMpID0+IHtcblx0aWYodHlwZW9mIFN5bWJvbCAhPT0gJ3VuZGVmaW5lZCcgJiYgU3ltYm9sLnRvU3RyaW5nVGFnKSB7XG5cdFx0T2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFN5bWJvbC50b1N0cmluZ1RhZywgeyB2YWx1ZTogJ01vZHVsZScgfSk7XG5cdH1cblx0T2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsICdfX2VzTW9kdWxlJywgeyB2YWx1ZTogdHJ1ZSB9KTtcbn07IiwiIiwiLy8gc3RhcnR1cFxuLy8gTG9hZCBlbnRyeSBtb2R1bGUgYW5kIHJldHVybiBleHBvcnRzXG4vLyBUaGlzIGVudHJ5IG1vZHVsZSB1c2VkICdtb2R1bGUnIHNvIGl0IGNhbid0IGJlIGlubGluZWRcbnZhciBfX3dlYnBhY2tfZXhwb3J0c19fID0gX193ZWJwYWNrX3JlcXVpcmVfXyhcIi4vc3JjL21haW4udHNcIik7XG4iLCIiXSwibmFtZXMiOltdLCJzb3VyY2VSb290IjoiIn0=
import PositionObserver from "@itihon/position-observer";

declare global {
    interface HTMLElement {
        browserDomId?: number
        lastBrowserRect?: SerializeRect|null
    }
}

let nextDomId = 1
interface SerializeRect {
    x: number
    y: number
    width : number
    height : number,
    captureScroll : boolean,
}
interface ChangeReport {
    targetId: number
    newRect : SerializeRect|null
}
let unreportedChanges : Map<number, ChangeReport> = new Map();
let reportCallback : ((_:any)=>void)|null = null;

function eq(rect : SerializeRect, other : SerializeRect){
    return rect.x == other.x && rect.y == other.y && rect.width == other.width && rect.height == other.height;
}

function reportIfPossible(){
    if(reportCallback != null && unreportedChanges.size > 0){
        reportCallback(Array.from(unreportedChanges.values()));
        unreportedChanges.clear();
        reportCallback = null;
    }
}

function reportChanged(target : HTMLElement, targetId : number, newRect : DOMRect|null){
    const serialized : SerializeRect|null = newRect ? ({
        x: newRect.x,
        y: newRect.y,
        width: newRect.width,
        height: newRect.height,
        captureScroll: target.classList.contains("browserScrollCapture")
    }) : null
    const oldSerialized = target.lastBrowserRect
    if(oldSerialized === undefined || ((serialized === null) !== (oldSerialized === null)) || (serialized != null && oldSerialized !== null && !eq(serialized, oldSerialized))){
        unreportedChanges.set(targetId,({
            targetId,
            newRect: serialized,
        }));
        target.lastBrowserRect = serialized;
        reportIfPossible();
    }
}
function getDomId(element : HTMLElement) : number|null {
    const id = element.browserDomId
    if(id){
        return id
    } else {
        return null
    }
}
function ensureDomId(element : HTMLElement) : number {
    const existing = getDomId(element)
    if(existing){ return existing }
    nextDomId += 1;
    element.browserDomId = nextDomId
    return nextDomId;
}



function onBoundsChanged(target : Element, targetRect : DOMRect, ctx : any){
    if(target instanceof HTMLElement){
        const domId = getDomId(target)
        if(domId != null){
            reportChanged(target, domId, targetRect)
        }
    }
}

const positionObserver = new PositionObserver(onBoundsChanged, {});

function startObserving(childNode : HTMLElement){
    const domId = ensureDomId(childNode);
    positionObserver.observe(childNode);
    reportChanged(childNode, domId, childNode.getBoundingClientRect())
}

const observer = new MutationObserver(mutations => {
    for(const mutation of mutations){
        if(mutation.type == "childList"){
            for(const childNode of mutation.addedNodes){
                if(childNode instanceof HTMLElement){
                    if(childNode.classList.contains("browserClickCapture")){
                        startObserving(childNode)
                    }
                }
            }
            for(const childNode of mutation.removedNodes){
                if(childNode instanceof HTMLElement){
                    const domId = getDomId(childNode)
                    if(domId != null){
                        positionObserver.unobserve(childNode);
                        reportChanged(childNode, domId, null);
                    }
                }
            }
        }
    }
});

for(const existing of document.querySelectorAll(".browserClickCapture")){
    if(existing instanceof HTMLElement){
        startObserving(existing)
    }
}
observer.observe(document.body, {childList: true, subtree: true});

(window as any).registerReportCallback = function(cb : ((_:any)=>void)){
    reportCallback = cb;
    reportIfPossible()
}
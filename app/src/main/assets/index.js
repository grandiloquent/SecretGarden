const bottomSheetContainer = document.querySelector('.bottom-sheet-container');

const bottomSheetScrim = document.querySelector('.bottom-sheet-scrim');
bottomSheetScrim.addEventListener('click', evt => {
    evt.preventDefault();
    evt.stopImmediatePropagation();
    bottomSheetContainer.style.display = 'none';
})
// const bottomSheetRenderer = document.querySelector('.bottom-sheet-renderer');
// bottomSheetRenderer.addEventListener('click', evt => {
//     evt.preventDefault();
//     bottomSheetContainer.style.display = 'block';

// })


const videoWithContextRenderer = document.querySelector('.video-with-context-renderer');
let mLimit = 20;
let mOffset = 0;
let mSearch=null;

function render() {

    if (typeof NativeAndroid !== 'undefined') {
        let uri = "";
        const videos = JSON.parse(NativeAndroid.loadVideos(mSearch,mSort,mVideoType, mLimit, mOffset));
        // if (!/91porn/.test(videos[0].thumbnail)) {
        //     uri = NativeAndroid.getRealAddress();
        // }
        mOffset += mLimit;
        const buffer = [];
        videos.forEach(video => {
            buffer.push(`<div class="media-item" data-id="${video.id}">
                    <div class="media-item-thumbnail-container">
                        <div class="video-thumbnail-container-large">
                            <div class="video-thumbnail-bg">
                            </div>
                            <img class="video-thumbnail-img lazy" src="${uri ? video.thumbnail.replace(/.+(?=\/images)/, uri) : video.thumbnail}">
                        </div>
                        <div class="time-display">
                            <div class="time-display-wrapper">
                                <div class="badge-shape">
                                ${formatDuration(video.duration)}
                                </div>
                            </div>
                        </div>
                    </div>
    
                    <div class="details">
                        <div class="media-channel" style="display:none">
                            <a>
                                <div class="profile-icon">
                                    <img class="profile-icon-image"
                                        src="https://yt3.ggpht.com/ytc/AIdro_mwTLOIJf5a-29E8ip454Dtebsq48ukFFQ9gh0J9iudI-w=s68-c-k-c0x00ffffff-no-rj">
                                </div>
                            </a>
                        </div>
                        <div class="media-item-info">
                            <div class="media-item-metadata">
                                <a>
                                    <h3 class="media-item-headline" data-id="${video.id}">${video.title}</h3>
                                    <div class="badge-and-byline-renderer">
                                        <span class="badge-and-byline-item-byline">
                                          ${video.views} 次
                                        </span>
                                        <span class="badge-and-byline-separator">•</span>
                                    <span class="badge-and-byline-item-byline">
                                    ${timeago(video.createAt)}
                                        </span>
                                        </div>
                                </a>
                            </div>
                            <div class="bottom-sheet-renderer" data-id="${video.id}">
                                <div class="button-shape">
                                    <button class="spec-button">
    
                                        <div class="c3-icon">
                                            <svg xmlns="http://www.w3.org/2000/svg" enable-background="new 0 0 24 24"
                                                height="24" viewBox="0 0 24 24" width="24" focusable="false"
                                                aria-hidden="true"
                                                style="pointer-events: none; display: inherit; width: 100%; height: 100%;">
                                                <path
                                                    d="M12 16.5c.83 0 1.5.67 1.5 1.5s-.67 1.5-1.5 1.5-1.5-.67-1.5-1.5.67-1.5 1.5-1.5zM10.5 12c0 .83.67 1.5 1.5 1.5s1.5-.67 1.5-1.5-.67-1.5-1.5-1.5-1.5.67-1.5 1.5zm0-6c0 .83.67 1.5 1.5 1.5s1.5-.67 1.5-1.5-.67-1.5-1.5-1.5-1.5.67-1.5 1.5z">
                                                </path>
                                            </svg>
                                        </div>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>`);
        })
        videoWithContextRenderer.insertAdjacentHTML('beforeend', buffer.join(''))
    }
    // var lazyLoadInstance = new LazyLoad({
    // });
    document.querySelectorAll('.media-item:not([binded])')
        .forEach(element => {
            element.setAttribute('binded', 'true');
            element.addEventListener('click', evt => {
                evt.stopPropagation();
                if (NativeAndroid != undefined)
                    NativeAndroid.play(parseInt(element.dataset.id));
            })
        });
    document.querySelectorAll('.bottom-sheet-renderer:not([binded])')
        .forEach(element => {
            element.setAttribute('binded', 'true');
            element.addEventListener('click', evt => {
                evt.stopPropagation();
                showActions(parseInt(element.dataset.id))
            })
        });
        document.querySelectorAll('.media-item-headline:not([binded])')
        .forEach(element => {
            element.setAttribute('binded', 'true');
            element.addEventListener('click', evt => {
                evt.stopPropagation();
                videoWithContextRenderer.innerHTML = '';
                mOffset = 0;
                render()
                NativeAndroid.moveVideo(parseInt(element.dataset.id))
            })
        });  
        
}


let mSort = (localStorage.getItem('sort') && parseInt(localStorage.getItem('sort'))) || 0;
let mVideoType = (localStorage.getItem('videoType') && parseInt(localStorage.getItem('videoType'))) || 1;

render();






const bottomSheetContent = document.querySelector('.bottom-sheet-content');
const bottomSheetLayoutContentWrapper = document.querySelector('.bottom-sheet-layout-content-wrapper');





function showActions(id) {
    bottomSheetContent.innerHTML = [
        "91",
        "57",
        "收藏",
        "屏蔽",
        "露脸",
        "其他",
        "刷新"].map((x, k) => {
            return `<div class="menu-item" data-id="${k + 1}">
                        <button class="menu-item-button">
                            <div class="c3-icon">

                            </div>
                            <span>${x}</span>
                        </button>
                    </div>`
        }).join('');
    bottomSheetLayoutContentWrapper.style.maxHeight = 'none'
    bottomSheetContainer.style.display = 'block';
    document.querySelectorAll('.menu-item')
        .forEach(element => {
            element.addEventListener('click', evt => {
                evt.stopPropagation();
                bottomSheetContainer.style.display = 'none';
                NativeAndroid.refreshVideo(id);
            })
        });
}









const searchQuery = document.querySelector('.search-query');
searchQuery.addEventListener('keydown', evt => {
    if (evt.keyCode === 13) {
        videoWithContextRenderer.innerHTML = '';
        mOffset = 0;
        mSearch=searchQuery.value;
        render()
    }
})



const clearButton = document.querySelector('.clear-button');
clearButton.addEventListener('click', evt => {
    evt.stopImmediatePropagation();
    searchQuery.value = ''
})

const searchButton = document.querySelector('.search-button');
searchButton.addEventListener('click', evt => {
    evt.preventDefault();
    evt.stopImmediatePropagation();
    videoWithContextRenderer.innerHTML = '';
    mOffset = 0;
    mSearch=searchQuery.value;
    render()
})

function showDialog(mode) {
    const div = document.createElement('div');
    div.innerHTML = `<div class="dialog-container">
        <div class="dialog">
       
            <div class="dialog-layout">
                <div class="dialog-header">
                    <div class="dialog-layout-title">
                        对话框
                    </div>
                </div>
                <div class="dialog-layout-container">
                    <div class="dialog-layout-content">
                        <div class="dialog-layout-content-inner">
                            <div style="display: flex;flex-direction: column;gap: 8px;font-size: 14px;">
                                <input class="dialog-input">
                                <input class="dialog-input">
                            </div>
                        </div>
                    </div>
                    <div class="dialog-layout-footer">
                        <div class="dialog-flex-button">
                            取消
                        </div>
                        <div class="dialog-flex-button grey">
                            确定
                        </div>
                    </div>
                </div>
            </div>
        </div>
         
    <div class="dialog-scrim">

    </div>
    </div>`;
    document.body.appendChild(div);
    div.querySelector('.dialog-flex-button:first-child')
        .addEventListener('click', evt => {
            evt.stopPropagation();
            evt.stopImmediatePropagation();
            div.remove();
        });
    const dialogInput1 = document.querySelector('.dialog-input:first-child');
    const dialogInput2 = document.querySelector('.dialog-input:last-child');


    div.querySelector('.dialog-flex-button:last-child')
        .addEventListener('click', evt => {
            evt.stopPropagation();
            evt.stopImmediatePropagation();
            div.remove();
            const start = (dialogInput1.value && parseInt(dialogInput1.value)) || 0;
            const end = (dialogInput2.value && parseInt(dialogInput2.value)) || 3;
            if (typeof NativeAndroid !== 'undefined') {
                NativeAndroid.fetchVideos(mode, start, end);
            }

        });

    const dialogScrim = div.querySelector('.dialog-scrim');
    dialogScrim.addEventListener('click', evt => {
        evt.stopPropagation();
        evt.preventDefault();
        evt.stopImmediatePropagation();
        div.remove();
    })

}






const videoOptions = document.querySelector('.video-options ');
videoOptions.addEventListener('click', evt => {
    evt.stopPropagation();
    evt.preventDefault();
    evt.stopImmediatePropagation();
    bottomSheetContent.innerHTML = ["91",
        "57"].map((x, k) => {
            return `<div class="menu-item" data-id="${k + 1}">
                        <button class="menu-item-button">
                            <div class="c3-icon">

                            </div>
                            <span>${x}</span>
                        </button>
                    </div>`
        }).join('');
    bottomSheetLayoutContentWrapper.style.maxHeight = 'none'
    bottomSheetContainer.style.display = 'block';
    document.querySelectorAll('.menu-item')
        .forEach(element => {
            element.addEventListener('click', evt => {
                evt.stopPropagation();
                bottomSheetContainer.style.display = 'none';
                const id = parseInt(element.dataset.id)
                if (id === 1) {
                    showDialog(1)
                } else if (id === 2) {
                    showDialog(2)
                }
            })
        });
})





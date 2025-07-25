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
                            最新
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
            if (typeof NativeAndroid !== 'undefined') {
                localStorage.setItem('fetch-mode', mode);
                NativeAndroid.fetchVideos(mode, 0, 10);
            }
        });
    const dialogInput1 = document.querySelector('.dialog-input:first-child');
    const dialogInput2 = document.querySelector('.dialog-input:last-child');


     
    div.querySelector('.dialog-flex-button:last-child')
        .addEventListener('click', evt => {
            evt.stopPropagation();
            evt.stopImmediatePropagation();
            div.remove();
            const start = (dialogInput1.value && parseInt(dialogInput1.value)) || 0;
            const end = (dialogInput2.value && parseInt(dialogInput2.value)) || (start + 1);
            if (typeof NativeAndroid !== 'undefined') {
                localStorage.setItem('fetch-mode', mode);
                localStorage.setItem('fetch-start'+mode, start);
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
        "57",
        "历史",
        "91",
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
                } else if (id === 3) {
                    const object = JSON.parse(localStorage.getItem('history') || "[]");
                    console.log(localStorage.getItem('history'));
                    let id = 0;
                    let founded = false;
                    for (let i = 0; i < object.length; i++) {
                        if (object[i]["videoType"] === mVideoType && object[i]["sort"] === mSort) {
                            id = object[i]["id"]
                            founded = true;
                            break;
                        }
                    }
                    if (!founded) {
                        return;
                    }
                    mOffset = NativeAndroid.indexOfVideo(mSort, mVideoType, id);
                    videoWithContextRenderer.innerHTML = '';

                    render()
                } else if (id === 4) {
                    NativeAndroid.getVideos(1, 0)
                } else if (id === 5) {
                    NativeAndroid.getVideos(2, 0)
                }
            })
        });
})

document.querySelector('.load-videos')
    .addEventListener('click', evt => {
        if (typeof NativeAndroid !== 'undefined') {
            const mode = parseInt(localStorage.getItem('fetch-mode')) || 1;
            const start = parseInt(localStorage.getItem('fetch-start'+mode)) || 0;
            NativeAndroid.fetchVideos(mode, start, start + 10);
        }
    })

document.querySelector('.increase-videos')
    .addEventListener('click', evt => {
        if (typeof NativeAndroid !== 'undefined') {
            const mode = parseInt(localStorage.getItem('fetch-mode')) || 1;
           
            const start = parseInt(localStorage.getItem('fetch-start'+mode)) || 0;
            localStorage.setItem('fetch-start'+mode, start + 10)
            evt.currentTarget.querySelector('.pivot-bar-item-title').textContent = start + 10;
        }
    })
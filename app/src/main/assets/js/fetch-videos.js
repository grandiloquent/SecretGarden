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





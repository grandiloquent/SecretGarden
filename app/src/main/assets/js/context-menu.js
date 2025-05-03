

function showActions(id,uri) {
    bottomSheetContent.innerHTML = [
        "91",
        "57",
        "收藏",
        "其他",
        "下载",
        "图片地址",
        "刷新",
        "记录"].map((x, k) => {
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
            element.addEventListener('click', async evt => {
                evt.stopPropagation();
                bottomSheetContainer.style.display = 'none';
                const index = parseInt(element.dataset.id);
                switch (index) {
                    case 1:
                        moveVideo(id, 1)
                        break;
                    case 2:
                        moveVideo(id, 2)
                        break;
                    case 3:
                        moveVideo(id, 3)
                        break;
                    case 4:
                        moveVideo(id, 6)
                        break;
                    case 5:
                        NativeAndroid.downloadVideo(id)
                        break;
                    case 6:
                      
                         
                        const response = await fetch(`${baseUri}/api/image`, {
                            method: 'PUT',
                            headers: {
                                'Content-Type': 'text/plain', // Or 'application/json' if you want to send JSON
                            },
                            body: uri,
                        });
                        response.text();
                        break;
                    case 7:
                        NativeAndroid.refreshVideo(id);
                        break;
                    case 8:
                        record(id);
                        break;
                }

            })
        });
}


function moveVideo(id, videoType) {
    NativeAndroid.moveVideo(id, videoType)
    const element = videoWithContextRenderer.querySelector('.media-item[data-id="' + id + '"]')
    element.remove();
}

function record(id) {
    const object = JSON.parse(localStorage.getItem('history') || "[]");
    let founded = false;
    for (let i = 0; i < object.length; i++) {
        if (object[i]["videoType"] === mVideoType && object[i]["sort"] === mSort) {
            object[i]["id"] = id;
            founded = true;
            break;
        }
    }
    if (!founded) {
        object.push({
            "videoType": mVideoType,
            "sort": mSort,
            "id": id
        })
    }
    localStorage.setItem('history', JSON.stringify(object));
}
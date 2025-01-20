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
let mSearch = null;
let mImageHost = localStorage.getItem("imageHost");
let mIsLoading = false;
const baseUri = window.location.host === "127.0.0.1:5500" ? "http://192.168.35.56:9100" : "";

async function render() {
    if (mIsLoading) return;
    mIsLoading = true;
    const videos = await (await fetch(`${baseUri}/api/videos?search=${encodeURIComponent(mSearch || '')}&sort=${mSort}&videoType=${mVideoType}&limit=${mLimit}&offset=${mOffset}`)).json();
    mIsLoading = false;
    mOffset += mLimit;
    var documentFragment = document.createDocumentFragment();
    videos.forEach(video => {


        const mediaItem = document.createElement('div');
        mediaItem.setAttribute("class", "media-item");
        mediaItem.setAttribute("data-id", video.id);

        const mediaItemThumbnailContainer = document.createElement('div');
        mediaItemThumbnailContainer.setAttribute("class", "media-item-thumbnail-container");
        mediaItem.appendChild(mediaItemThumbnailContainer);
        const videoThumbnailContainerLarge = document.createElement('div');
        videoThumbnailContainerLarge.setAttribute("class", "video-thumbnail-container-large");
        mediaItemThumbnailContainer.appendChild(videoThumbnailContainerLarge);
        const videoThumbnailBg = document.createElement('div');
        videoThumbnailBg.setAttribute("class", "video-thumbnail-bg");
        videoThumbnailContainerLarge.appendChild(videoThumbnailBg);
        const videoThumbnailImgLazy = document.createElement('img');
        videoThumbnailImgLazy.setAttribute("class", "video-thumbnail-img lazy");
        videoThumbnailImgLazy.setAttribute("src", (mImageHost && video.thumbnail.indexOf(".xyz/") !== -1) ? video.thumbnail.replace(/.+(?=\/images)/, mImageHost) : video.thumbnail);
        videoThumbnailContainerLarge.appendChild(videoThumbnailImgLazy);
        const timeDisplay = document.createElement('div');
        timeDisplay.setAttribute("class", "time-display");
        mediaItemThumbnailContainer.appendChild(timeDisplay);
        const timeDisplayWrapper = document.createElement('div');
        timeDisplayWrapper.setAttribute("class", "time-display-wrapper");
        timeDisplay.appendChild(timeDisplayWrapper);
        const badgeShape = document.createElement('div');
        badgeShape.setAttribute("class", "badge-shape");
        timeDisplayWrapper.appendChild(badgeShape);
        badgeShape.textContent = formatDuration(video.duration);
        const details = document.createElement('div');
        details.setAttribute("class", "details");
        mediaItem.appendChild(details);
        const mediaChannel = document.createElement('div');
        mediaChannel.setAttribute("class", "media-channel");
        mediaChannel.style.display = "none";
        details.appendChild(mediaChannel);
        const a = document.createElement('a');
        mediaChannel.appendChild(a);
        const profileIcon = document.createElement('div');
        profileIcon.setAttribute("class", "profile-icon");
        a.appendChild(profileIcon);
        const profileIconImage = document.createElement('img');
        profileIconImage.setAttribute("class", "profile-icon-image");
        profileIconImage.setAttribute("src", "https://yt3.ggpht.com/ytc/AIdro_mwTLOIJf5a-29E8ip454Dtebsq48ukFFQ9gh0J9iudI-w=s68-c-k-c0x00ffffff-no-rj");
        profileIcon.appendChild(profileIconImage);
        const mediaItemInfo = document.createElement('div');
        mediaItemInfo.setAttribute("class", "media-item-info");
        details.appendChild(mediaItemInfo);
        const mediaItemMetadata = document.createElement('div');
        mediaItemMetadata.setAttribute("class", "media-item-metadata");
        mediaItemInfo.appendChild(mediaItemMetadata);
        const a1 = document.createElement('a');
        mediaItemMetadata.appendChild(a1);
        const mediaItemHeadline = document.createElement('h3');
        mediaItemHeadline.setAttribute("class", "media-item-headline");
        mediaItemHeadline.setAttribute("data-id", video.id);
        a1.appendChild(mediaItemHeadline);
        mediaItemHeadline.textContent = video.title;
        const badgeAndBylineRenderer = document.createElement('div');
        badgeAndBylineRenderer.setAttribute("class", "badge-and-byline-renderer");
        a1.appendChild(badgeAndBylineRenderer);
        const badgeAndBylineItemByline = document.createElement('span');
        badgeAndBylineItemByline.setAttribute("class", "badge-and-byline-item-byline");
        badgeAndBylineRenderer.appendChild(badgeAndBylineItemByline);
        badgeAndBylineItemByline.textContent = `${video.views} 次`;
        const badgeAndBylineSeparator = document.createElement('span');
        badgeAndBylineSeparator.setAttribute("class", "badge-and-byline-separator");
        badgeAndBylineRenderer.appendChild(badgeAndBylineSeparator);
        badgeAndBylineSeparator.textContent = `•`;
        const badgeAndBylineItemByline2 = document.createElement('span');
        badgeAndBylineItemByline2.setAttribute("class", "badge-and-byline-item-byline");
        badgeAndBylineRenderer.appendChild(badgeAndBylineItemByline2);
        badgeAndBylineItemByline2.textContent = timeago(video.createAt);
        const bottomSheetRenderer = document.createElement('div');
        bottomSheetRenderer.setAttribute("class", "bottom-sheet-renderer");
        bottomSheetRenderer.setAttribute("data-id", "127309");
        bottomSheetRenderer.setAttribute("binded", "true");
        mediaItemInfo.appendChild(bottomSheetRenderer);
        const buttonShape = document.createElement('div');
        buttonShape.setAttribute("class", "button-shape");
        bottomSheetRenderer.appendChild(buttonShape);
        const specButton = document.createElement('button');
        specButton.setAttribute("class", "spec-button");
        buttonShape.appendChild(specButton);
        const c3Icon = document.createElement('div');
        c3Icon.setAttribute("class", "c3-icon");
        specButton.appendChild(c3Icon);
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute("xmlns", "http://www.w3.org/2000/svg");
        svg.setAttribute("enable-background", "new 0 0 24 24");
        svg.setAttribute("height", "24");
        svg.setAttribute("viewBox", "0 0 24 24");
        svg.setAttribute("width", "24");
        svg.setAttribute("focusable", "false");
        svg.setAttribute("aria-hidden", "true");
        svg.style.pointerEvents = "none";
        svg.style.display = "inherit";
        svg.style.width = "100%";
        svg.style.height = "100%";
        c3Icon.appendChild(svg);
        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        path.setAttribute("d", "M12 16.5c.83 0 1.5.67 1.5 1.5s-.67 1.5-1.5 1.5-1.5-.67-1.5-1.5.67-1.5 1.5-1.5zM10.5 12c0 .83.67 1.5 1.5 1.5s1.5-.67 1.5-1.5-.67-1.5-1.5-1.5-1.5.67-1.5 1.5zm0-6c0 .83.67 1.5 1.5 1.5s1.5-.67 1.5-1.5-.67-1.5-1.5-1.5-1.5.67-1.5 1.5z");
        svg.appendChild(path);

        documentFragment.appendChild(mediaItem);
        mediaItem.addEventListener('click', evt => {
            evt.stopPropagation();
            if (NativeAndroid != undefined)
                NativeAndroid.play(video.id);
        })
        bottomSheetRenderer.addEventListener('click', evt => {
            evt.stopPropagation();
            showActions(video.id)
        })

        mediaItemHeadline.addEventListener('click', async evt => {
            evt.stopPropagation();
            const id = video.id;
            const response = await fetch(`${baseUri}/api/video?id=${id}`, {
                method: 'PUT'
            });
            const results = await response.json();
            try {
                if (!results) return;
                mediaItemHeadline.textContent = results[0];
                document.querySelector('.media-item[data-id="' + id + '"] img.video-thumbnail-img').src = results[1];
            } catch (error) {
                console.log(error)
            }

            //moveVideo(parseInt(element.dataset.id), 6)
        })
    })
    videoWithContextRenderer.appendChild(documentFragment)

}


let mSort = (localStorage.getItem('sort') && parseInt(localStorage.getItem('sort'))) || 0;
let mVideoType = (localStorage.getItem('videoType') && parseInt(localStorage.getItem('videoType'))) || 1;

async function initialize() {
    await render();
}
initialize();




const bottomSheetContent = document.querySelector('.bottom-sheet-content');
const bottomSheetLayoutContentWrapper = document.querySelector('.bottom-sheet-layout-content-wrapper');



window.addEventListener('scroll', evt => {
    const element = document.querySelector('.media-item:not([view])');
    if (element.getBoundingClientRect().top < 47) {
        element.setAttribute('view', '')
        const id = parseInt(element.dataset.id);
        fetch(`${baseUri}/api/video?id=${id}`)
    }
})












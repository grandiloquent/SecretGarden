(() => {
    const strings = `accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9
    accept-encoding: gzip, deflate, br
    accept-language: en
    cache-control: no-cache
    pragma: no-cache
    referer: https://cableav.tv/category/chinese-live-porn/
    sec-fetch-dest: document
    sec-fetch-mode: navigate
    sec-fetch-site: same-origin
    sec-fetch-user: ?1
    upgrade-insecure-requests: 1
    user-agent: Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1`
    console.log(strings.split('\n').map(x => {
        const pieces = x.split(':');
        return [pieces[0].trim(), x.substring(pieces[0].length + 1).trim()]
    }).map(x=>{
        return `c.addRequestProperty("${x[0]}","${x[1]}");`
    }).join("\n"))
})();
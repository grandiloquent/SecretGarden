#ifndef NATIVE_LIB_H
#define NATIVE_LIB_H
// #include "native-lib.h"
// #define DOT_OR_DOTDOT(s) ((s)[0] == '.' && (!(s)[1] || ((s)[1] == '.' && !(s)[2])))
#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, "B5aOx2::", __VA_ARGS__))
#define LOGE(...) \
  ((void)__android_log_print(ANDROID_LOG_ERROR, "B5aOx2::", __VA_ARGS__))

static uint64_t _linux_get_time_ms(void) {
    struct timeval tv = {0};
    uint64_t time_ms;

    gettimeofday(&tv, NULL);

    time_ms = tv.tv_sec * 1000 + tv.tv_usec / 1000;

    return time_ms;
}
static uint64_t _linux_time_left(uint64_t t_end, uint64_t t_now) {
    uint64_t t_left;

    if (t_end > t_now) {
        t_left = t_end - t_now;
    } else {
        t_left = 0;
    }

    return t_left;
}

uintptr_t HAL_TCP_Connect(const char *host, uint16_t port) {
    int ret;
    struct addrinfo hints, *addr_list, *cur;
    int fd = 0;

    char port_str[6];
    snprintf(port_str, 6, "%d", port);

    memset(&hints, 0x00, sizeof(hints));
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;

    ret = getaddrinfo(host, port_str, &hints, &addr_list);
    if (ret) {
//        if (ret == EAI_SYSTEM)
//            Log_e("getaddrinfo(%s:%s) error: %s", STRING_PTR_PRINT_SANITY_CHECK(host), port_str, strerror(errno));
//        else
//            Log_e("getaddrinfo(%s:%s) error: %s", STRING_PTR_PRINT_SANITY_CHECK(host), port_str, gai_strerror(ret));
//
        return 0;
    }

    for (cur = addr_list; cur != NULL; cur = cur->ai_next) {
        fd = (int) socket(cur->ai_family, cur->ai_socktype, cur->ai_protocol);
        if (fd < 0) {
            ret = 0;
            continue;
        }

        if (connect(fd, cur->ai_addr, cur->ai_addrlen) == 0) {
            ret = fd;
            break;
        }

        close(fd);
        ret = 0;
    }

    /*
    if (0 == ret) {
        Log_e("fail to connect with TCP server: %s:%s", STRING_PTR_PRINT_SANITY_CHECK(host), port_str);
    } else {
     reduce log print due to frequent log server connect/disconnect
        if (strstr(host, LOG_UPLOAD_SERVER_PATTEN))
            UPLOAD_DBG("connected with TCP server: %s:%s", STRING_PTR_PRINT_SANITY_CHECK(host), port_str);
        else
            Log_i("connected with TCP server: %s:%s", STRING_PTR_PRINT_SANITY_CHECK(host), port_str);
    }
    */

    freeaddrinfo(addr_list);

    return (uintptr_t) ret;
}

int HAL_TCP_Write(uintptr_t fd, const unsigned char *buf, uint32_t len, uint32_t timeout_ms,
                  size_t *written_len) {
    int ret;
    uint32_t len_sent;
    uint64_t t_end, t_left;
    fd_set sets;

    t_end = _linux_get_time_ms() + timeout_ms;
    len_sent = 0;

    /* send one time if timeout_ms is value 0 */
    do {
        t_left = _linux_time_left(t_end, _linux_get_time_ms());

        if (0 != t_left) {
            struct timeval timeout;

            FD_ZERO(&sets);
            FD_SET(fd, &sets);

            timeout.tv_sec = t_left / 1000;
            timeout.tv_usec = (t_left % 1000) * 1000;

            ret = select(fd + 1, NULL, &sets, NULL, &timeout);
            if (ret > 0) {
                if (0 == FD_ISSET(fd, &sets)) {
                    //Log_e("Should NOT arrive");
                    /* If timeout in next loop, it will not sent any data */
                    ret = 0;
                    continue;
                }
            } else if (0 == ret) {
                ret = -605;
                // Log_e("select-write timeout %d", (int)fd);
                break;
            } else {
                if (EINTR == errno) {
                    // Log_e("EINTR be caught");
                    continue;
                }

                ret = -607;
                //Log_e("select-write fail: %s", STRING_PTR_PRINT_SANITY_CHECK(strerror(errno)));
                break;
            }
        } else {
            ret = -605;
        }

        if (ret > 0) {
            ret = send(fd, buf + len_sent, len - len_sent, 0);
            if (ret > 0) {
                len_sent += ret;
            } else if (0 == ret) {
                //  Log_e("No data be sent. Should NOT arrive");
            } else {
                if (EINTR == errno) {
                    //    Log_e("EINTR be caught");
                    continue;
                }

                ret = -607;
                // Log_e("send fail: %s", STRING_PTR_PRINT_SANITY_CHECK(strerror(errno)));
                break;
            }
        }
    } while ((len_sent < len) && (_linux_time_left(t_end, _linux_get_time_ms()) > 0));

    *written_len = (size_t) len_sent;

    return len_sent > 0 ? 0 : ret;
}

int HAL_TCP_Read(uintptr_t fd, unsigned char *buf, uint32_t len, uint32_t timeout_ms,
                 size_t *read_len) {
    int ret, err_code;
    uint32_t len_recv;
    uint64_t t_end, t_left;
    fd_set sets;
    struct timeval timeout;

    t_end = _linux_get_time_ms() + timeout_ms;
    len_recv = 0;
    err_code = 0;

    do {
        t_left = _linux_time_left(t_end, _linux_get_time_ms());
        if (0 == t_left) {
            err_code = -604;
            break;
        }

        FD_ZERO(&sets);
        FD_SET(fd, &sets);

        timeout.tv_sec = t_left / 1000;
        timeout.tv_usec = (t_left % 1000) * 1000;

        ret = select(fd + 1, &sets, NULL, NULL, &timeout);
        if (ret > 0) {
            ret = recv(fd, buf + len_recv, len - len_recv, 0);
            if (ret > 0) {
                len_recv += ret;
            } else if (0 == ret) {
                struct sockaddr_in peer;
                socklen_t sLen = sizeof(peer);
                int peer_port = 0;
                getpeername(fd, (struct sockaddr *) &peer, &sLen);
                peer_port = ntohs(peer.sin_port);

                /* reduce log print due to frequent log server connect/disconnect */
//                if (peer_port == LOG_UPLOAD_SERVER_PORT)
//                    UPLOAD_DBG("connection is closed by server: %s:%d",
//                               STRING_PTR_PRINT_SANITY_CHECK(inet_ntoa(peer.sin_addr)), peer_port);
//                else
//                    Log_e("connection is closed by server: %s:%d",
//                          STRING_PTR_PRINT_SANITY_CHECK(inet_ntoa(peer.sin_addr)), peer_port);

                err_code = -608;
                break;
            } else {
                if (EINTR == errno) {
                    // Log_e("EINTR be caught");
                    continue;
                }
                //Log_e("recv error: %s", STRING_PTR_PRINT_SANITY_CHECK(strerror(errno)));
                err_code = -606;
                break;
            }
        } else if (0 == ret) {
            err_code = -604;
            break;
        } else {
            //Log_e("select-recv error: %s", STRING_PTR_PRINT_SANITY_CHECK(strerror(errno)));
            err_code = -606;
            break;
        }
    } while ((len_recv < len));

    *read_len = (size_t) len_recv;

    if (err_code == -604 && len_recv == 0)
        err_code = -609;

    return (len == len_recv) ? 0 : err_code;
}

int HAL_TCP_Disconnect(uintptr_t fd) {
    int rc;

    /* Shutdown both send and receive operations. */
    rc = shutdown((int) fd, 2);
    if (0 != rc) {
        // Log_e("shutdown error: %s", STRING_PTR_PRINT_SANITY_CHECK(strerror(errno)));
        return -1;
    }

    rc = close((int) fd);
    if (0 != rc) {
        // Log_e("closesocket error: %s", STRING_PTR_PRINT_SANITY_CHECK(strerror(errno)));
        return -1;
    }

    return 0;
}


int substring(const char *s, const char *start, const char *end, char *dst) {
    char *s1 = strstr(s, start);
    if (s1 == NULL)return -1;
    s1 += strlen(start);
    char *s2 = strstr(s, end);
    if (s2 == NULL)return -1;
    dst[0] = 0;
    strncat(dst, s1, s2 - s1);
    return 0;
}

// https://android.googlesource.com/platform/system/core/+/jb-dev/toolbox/rm.c
/* return -1 on failure, with errno set to the first error */
static int unlink_recursive(const char* name)
{
    struct stat st;
    DIR *dir;
    struct dirent *de;
    int fail = 0;
    /* is it a file or directory? */
    if (lstat(name, &st) < 0)
        return -1;
    /* a file, so unlink it */
    if (!S_ISDIR(st.st_mode))
        return unlink(name);
    /* a directory, so open handle */
    dir = opendir(name);
    if (dir == NULL)
        return -1;
    /* recurse over components */
    errno = 0;
    while ((de = readdir(dir)) != NULL) {
        char dn[PATH_MAX];
        if (!strcmp(de->d_name, "..") || !strcmp(de->d_name, "."))
            continue;
        sprintf(dn, "%s/%s", name, de->d_name);
        if (unlink_recursive(dn) < 0) {
            fail = 1;
            break;
        }
        errno = 0;
    }
    /* in case readdir or unlink_recursive failed */
    if (fail || errno < 0) {
        int save = errno;
        closedir(dir);
        errno = save;
        return -1;
    }
    /* close directory handle */
    if (closedir(dir) < 0)
        return -1;
    /* delete target directory */
    return rmdir(name);
}
#endif

#!/usr/bin/env python
import pty, os, select, sys

master_fd, slave_fd = pty.openpty()
proc = os.fork()
if proc == 0:
    os.close(master_fd)
    os.setsid()
    os.dup2(slave_fd, 0)
    os.dup2(slave_fd, 1)
    os.dup2(slave_fd, 2)
    if slave_fd > 2:
        os.close(slave_fd)
    os.execv('./tools/ast_distance', [
        './tools/ast_distance', '--deep',
        'tmp/url/src', 'rust',
        'src/commonMain/kotlin/io/github/kotlinmania/url', 'kotlin',
    ])
    os._exit(1)

os.close(slave_fd)
output = []
while True:
    r, _, _ = select.select([master_fd], [], [], 0.1)
    if r:
        try:
            data = os.read(master_fd, 65536)
            if not data:
                break
            output.append(data)
        except OSError:
            break
    else:
        pid, status = os.waitpid(proc, os.WNOHANG)
        if pid != 0:
            break
sys.stdout.buffer.write(b''.join(output))

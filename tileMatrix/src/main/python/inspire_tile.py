import morecantile

_tms = morecantile.tms.get("WorldCRS84Quad")


def get_tile(lon, lat, z):
    return _tms.tile(lon, lat, z)


def get_tile_bbox(tile):
    return _tms.bounds(tile)


def get_tiles(left, bottom, right, top, z):
    left_bottom = _tms.tile(left, bottom, z)
    right_top = _tms.tile(right, top, z)

    result = []
    for x in range(left_bottom.x, right_top.x + 1):
        for y in range(right_top.y, left_bottom.y + 1):
            result.append(morecantile.Tile(x, y, z))

    return result


def get_bboxes(left, bottom, right, top, z):
    result = []
    for tile in get_tiles(left, bottom, right, top, z):
        result.append(_tms.bounds(tile))

    return result

def get_tiles_n_bboxes(left, bottom, right, top, z):
    result = []
    for tile in get_tiles(left, bottom, right, top, z):
        result.append([tile, _tms.bounds(tile)])

    return result

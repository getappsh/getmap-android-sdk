import morecantile

_tms = morecantile.tms.get("WorldCRS84Quad")


def get_tile(lon, lat, z):
    t = _tms.tile(lon, lat, z)
    b = _tms.bounds(t)
    return t, b
